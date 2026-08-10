package de.pocketcloud.shared.component.software;

import de.pocketcloud.api.component.software.ISoftwareBinary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Basically, the URL has to be a download link for either a .zip or .gz file.
 * This file should contain all the relevant binary files inside one single root folder (name can be anything)
 * Example:
 * php-8.4.tar.gz
 * -> bin
 * -> php7
 * -> bin
 * -> [php.exe, php.ini, etc...]
 * <p>
 * This `bin` folder will then be extracted inside the software/{software}/binary/ folder.
 * If the url is either null or blank, `java` will be the result of the placeholder {BINARY_PATH}
 * inside the `download` field in the respective config.
 */

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SoftwareBinary implements ISoftwareBinary {

    protected final String url;
    protected final boolean checkForUpdates;
}