package io.github.mcvalac.extension.defaults.bukkit.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility for serializing and deserializing arrays of {@link ItemStack} using
 * the Bukkit object streams.
 * <p>
 * This replaces the Paper-only {@code ItemStack.serializeItemsAsBytes} /
 * {@code ItemStack.deserializeItemsFromBytes} helpers so the module can build
 * against the plain Spigot API. The wire format is a leading {@code int} length
 * followed by that many {@link ItemStack} objects written via the Bukkit object
 * stream.
 * </p>
 */
public final class InventorySerialization {

    private InventorySerialization() {
    }

    /**
     * Serializes the given items into a byte array.
     *
     * @param items The items to serialize (slots may be {@code null}).
     * @return The serialized bytes.
     * @throws IOException If an IO error occurs during serialization.
     */
    public static byte[] toBytes(ItemStack[] items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Deserializes a byte array back into an array of items.
     *
     * @param data The serialized bytes.
     * @return The reconstructed items (slots may be {@code null}).
     * @throws IOException If an IO error occurs during deserialization.
     */
    public static ItemStack[] fromBytes(byte[] data) throws IOException {
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
