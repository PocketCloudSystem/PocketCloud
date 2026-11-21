package de.pocketcloud.cloud.cluster.security;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Cluster PKI: a private cluster CA signs one certificate per node (CN = node name).
 * Node connections use mutual TLS, so only nodes with a certificate of this CA can join.
 * <p>
 * Files in {@code storage/internal/cluster/}:
 * {@code ca.crt} (all nodes), {@code ca.key} (only the node that created the CA, needed for {@code node add}),
 * {@code node.crt} and {@code node.key} (this node).
 * <p>
 * Keys are EC P-256 and generated with the JDK provider; Bouncy Castle is only used to build and parse
 * certificates and PEM files.
 */
public final class ClusterCertificates {

    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final Duration CA_VALIDITY = Duration.ofDays(3650);
    private static final Duration NODE_VALIDITY = Duration.ofDays(825);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path directory;
    private X509Certificate caCertificate;
    private PrivateKey caKey;
    private X509Certificate nodeCertificate;
    private PrivateKey nodeKey;

    public ClusterCertificates(Path directory) {
        this.directory = directory;
    }

    /**
     * @return true if this node has everything it needs to take part in a cluster
     */
    public boolean load() throws IOException, GeneralSecurityException {
        if (!Files.isRegularFile(caCertFile()) || !Files.isRegularFile(nodeCertFile()) || !Files.isRegularFile(nodeKeyFile())) return false;
        caCertificate = parseCertificate(Files.readString(caCertFile()));
        nodeCertificate = parseCertificate(Files.readString(nodeCertFile()));
        nodeKey = parsePrivateKey(Files.readString(nodeKeyFile()));
        caKey = Files.isRegularFile(caKeyFile()) ? parsePrivateKey(Files.readString(caKeyFile())) : null;
        return true;
    }

    /**
     * Creates a new cluster CA and a certificate for this node.
     */
    public void initialize(String nodeName, String cloudName) throws IOException, GeneralSecurityException {
        KeyPair caPair = generateKeyPair();
        X500Name caSubject = new X500Name("CN=PocketCloud Cluster CA " + cloudName);
        Instant now = Instant.now();

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                caSubject, serial(), Date.from(now.minus(Duration.ofMinutes(5))), Date.from(now.plus(CA_VALIDITY)),
                caSubject, caPair.getPublic());
        JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(caPair.getPublic()));

        caCertificate = toCertificate(builder.build(signer(caPair.getPrivate())));
        caKey = caPair.getPrivate();

        IssuedNode own = issue(nodeName);
        nodeCertificate = own.certificate();
        nodeKey = own.key();

        Files.createDirectories(directory);
        writeSecret(caKeyFile(), toPem(caKey));
        Files.writeString(caCertFile(), toPem(caCertificate));
        Files.writeString(nodeCertFile(), toPem(nodeCertificate));
        writeSecret(nodeKeyFile(), toPem(nodeKey));
    }

    /**
     * Issues a certificate for another node. Requires the CA key.
     */
    public IssuedNode issue(String nodeName) throws GeneralSecurityException, IOException {
        if (caKey == null) throw new IllegalStateException("This node does not hold the cluster CA key (ca.key).");
        KeyPair pair = generateKeyPair();
        Instant now = Instant.now();
        X500Name issuer = new JcaX509CertificateHolder(caCertificate).getSubject();

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial(), Date.from(now.minus(Duration.ofMinutes(5))), Date.from(now.plus(NODE_VALIDITY)),
                new X500Name("CN=" + nodeName), pair.getPublic());
        JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyAgreement));
        builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));
        builder.addExtension(Extension.authorityKeyIdentifier, false, ext.createAuthorityKeyIdentifier(caCertificate));

        return new IssuedNode(nodeName, toCertificate(builder.build(signer(caKey))), pair.getPrivate());
    }

    /**
     * Stores the material of a join token as this node's identity.
     */
    public void install(JoinToken token) throws IOException, GeneralSecurityException {
        Files.createDirectories(directory);
        Files.writeString(caCertFile(), token.caCertificatePem());
        Files.writeString(nodeCertFile(), token.nodeCertificatePem());
        writeSecret(nodeKeyFile(), token.nodeKeyPem());
        load();
    }

    public SslContext serverContext() throws SSLException {
        return SslContextBuilder.forServer(nodeKey, nodeCertificate)
                .trustManager(caCertificate)
                .clientAuth(ClientAuth.REQUIRE)
                .protocols("TLSv1.3")
                .build();
    }

    public SslContext clientContext() throws SSLException {
        return SslContextBuilder.forClient()
                .keyManager(nodeKey, nodeCertificate)
                .trustManager(caCertificate)
                // Nodes are identified by the certificate CN (checked against the hello), not by host names.
                .endpointIdentificationAlgorithm(null)
                .protocols("TLSv1.3")
                .build();
    }

    public boolean hasCaKey() {
        return caKey != null;
    }

    public X509Certificate caCertificate() {
        return caCertificate;
    }

    public X509Certificate nodeCertificate() {
        return nodeCertificate;
    }

    /**
     * Common name of a peer certificate, which must equal the node name.
     */
    public static String commonName(X509Certificate certificate) {
        X500Name name = new X500Name(certificate.getSubjectX500Principal().getName());
        var rdns = name.getRDNs(BCStyle.CN);
        if (rdns.length == 0) return null;
        return IETFUtils.valueToString(rdns[0].getFirst().getValue());
    }

    public static String serialOf(X509Certificate certificate) {
        return certificate.getSerialNumber().toString(16);
    }

    // ------------------------------------------------------------------ PEM helpers

    public static String toPem(Object object) throws IOException {
        StringWriter out = new StringWriter();
        try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
            // Private keys as PKCS#8, which keeps the public key derivable for every key type.
            if (object instanceof PrivateKey key) writer.writeObject(new JcaPKCS8Generator(key, null));
            else writer.writeObject(object);
        }
        return out.toString();
    }

    public static X509Certificate parseCertificate(String pem) throws IOException, GeneralSecurityException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (!(obj instanceof X509CertificateHolder holder)) throw new IOException("Not a certificate");
            return new JcaX509CertificateConverter().getCertificate(holder);
        }
    }

    public static PrivateKey parsePrivateKey(String pem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (obj instanceof PEMKeyPair pair) return converter.getKeyPair(pair).getPrivate();
            if (obj instanceof PrivateKeyInfo info) return converter.getPrivateKey(info);
            throw new IOException("Not a private key");
        }
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    private static ContentSigner signer(PrivateKey key) throws GeneralSecurityException {
        try {
            return new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(key);
        } catch (org.bouncycastle.operator.OperatorCreationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    private static X509Certificate toCertificate(X509CertificateHolder holder) throws GeneralSecurityException {
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private static BigInteger serial() {
        return new BigInteger(127, RANDOM);
    }

    private static void writeSecret(Path file, String content) throws IOException {
        Files.writeString(file, content);
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private Path caCertFile() {
        return directory.resolve("ca.crt");
    }

    private Path caKeyFile() {
        return directory.resolve("ca.key");
    }

    private Path nodeCertFile() {
        return directory.resolve("node.crt");
    }

    private Path nodeKeyFile() {
        return directory.resolve("node.key");
    }

    public record IssuedNode(String name, X509Certificate certificate, PrivateKey key) {}
}
