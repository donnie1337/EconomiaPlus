package com.coinseconomy.plugin.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class AtomicYamlSaver {

    private AtomicYamlSaver() {
    }

    public static void save(FileConfiguration configuration, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();

        File temp = new File(parent, target.getName() + ".tmp");
        configuration.save(temp);

        try {
            Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
