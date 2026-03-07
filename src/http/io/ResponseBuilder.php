<?php

namespace pocketcloud\cloud\http\io;

use GdImage;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\http\util\StatusCode;

final class ResponseBuilder {

    public static function create(): self {
        return new self;
    }

    private int $statusCode = 200;
    private string $body = "";

    private ?string $customMessage = null;

    private array $headers = ["Content-Type" => "application/json", "Content-Length" => 0, "Connection" => "close"];

    public function code(int|StatusCode $statusCode): self {
        $this->statusCode = ($statusCode instanceof StatusCode ? $statusCode->value : $statusCode);
        return $this;
    }

    public function body(string|array $body): self {
        $this->body = (is_array($body) ? json_encode($body) : $body);
        if (is_array($body)) $this->contentType("application/json");
        else if (is_string($body)) $this->contentType("text/plain");
        $this->headers["Content-Length"] = strlen($this->body);
        return $this;
    }

    public function image(GdImage $image): self {
        ob_start();
        imagepng($image);
        $pngData = ob_get_clean();
        imagedestroy($image);
        $this->body($pngData);
        $this->contentType("image/png");
        return $this;
    }

    public function html(string $body): self {
        $this->contentType("text/html");
        $this->body($body);
        return $this;
    }

    public function redirect(string $url, bool $update_body = true): self {
        $url = str_replace(["\r", "\n"], "", $url);
        $this->headers["Location"] = $url;
        $this->code(302);
        if ($update_body) {
            $escapedUrl = htmlspecialchars($url, ENT_QUOTES, "UTF-8");
            $this->html("<p>Redirecting to <a href='" . $escapedUrl . "'>" . $escapedUrl . "</a></p>");
        }
        return $this;
    }

    public function contentType(string $type): self {
        $type = str_replace(["\r", "\n"], "", $type);
        $this->headers["Content-Type"] = $type;
        return $this;
    }

    public function customMessage(string $message): self {
        $this->customMessage = $message;
        return $this;
    }

    public function build(): Response {
        return new Response($this->statusCode, $this->body, $this->customMessage, ThreadSafeArray::fromArray($this->headers));
    }
}