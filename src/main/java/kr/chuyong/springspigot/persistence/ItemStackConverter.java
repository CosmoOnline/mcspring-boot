package kr.chuyong.springspigot.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Converter(autoApply = true)
public class ItemStackConverter implements AttributeConverter<ItemStack, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn(ItemStack attribute) {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); BukkitObjectOutputStream buos = new BukkitObjectOutputStream(baos)) {
            buos.writeObject(attribute);
            return baos.toByteArray();
        }catch(Exception ex) {
            throw new RuntimeException();
        }
    }

    @Override
    public ItemStack convertToEntityAttribute(byte[] dbData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(dbData); BukkitObjectInputStream buis = new BukkitObjectInputStream(bais)) {
            return (ItemStack) buis.readObject();
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }
}
