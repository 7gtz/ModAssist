package sevengtz.autochatmod.config;

import net.minecraft.text.Text;

/**
 * Enumeration of available sound options for alerts.
 * Each option maps to a Minecraft sound identifier.
 */
public enum SoundOption {
    // Standard & UI Sounds
    EXPERIENCE_ORB("Experience Orb", "minecraft:entity.experience_orb.pickup"),
    LEVEL_UP("Level Up", "minecraft:entity.player.levelup"),
    ITEM_PICKUP("Item Pickup", "minecraft:entity.item.pickup"),
    NOTE_PLING("Note Block 'Pling'", "minecraft:block.note_block.pling"),

    // Block Sounds
    BELL_RING("Bell Ring", "minecraft:block.bell.use"),
    ANVIL_LAND("Anvil Land", "minecraft:block.anvil.land"),
    AMETHYST_CHIME("Amethyst Chime", "minecraft:block.amethyst_block.chime"),
    ENDER_CHEST("Ender Chest Close", "minecraft:block.ender_chest.close"),
    TRIPWIRE("Tripwire Click", "minecraft:block.tripwire.click_on"),
    GOAT_HORN("Goat Horn", "minecraft:item.goat_horn.sound.0"),

    // Neutral Mob Sounds
    VILLAGER_NO("Villager 'No'", "minecraft:entity.villager.no"),
    VILLAGER_YES("Villager 'Yes'", "minecraft:entity.villager.yes"),
    CAT_MEOW("Cat Meow", "minecraft:entity.cat.purr"),
    IRON_GOLEM_REPAIR("Golem Repair", "minecraft:entity.iron_golem.repair"),

    // Hostile & Impactful Sounds
    BLAZE_SHOOT("Blaze Shoot", "minecraft:entity.blaze.shoot"),
    ENDERMAN_TELEPORT("Enderman Teleport", "minecraft:entity.enderman.teleport"),
    GHAST_WARN("Ghast Warn", "minecraft:entity.ghast.warn"),
    GUARDIAN_ATTACK("Guardian Beam", "minecraft:entity.guardian.attack"),
    WITHER_SPAWN("Wither Spawn", "minecraft:entity.wither.spawn"),
    ENDER_DRAGON_GROWL("Dragon Growl", "minecraft:entity.ender_dragon.growl");

    private final String displayName;
    private final String soundId;

    SoundOption(String displayName, String soundId) {
        this.displayName = displayName;
        this.soundId = soundId;
    }

    /**
     * Gets the Minecraft sound identifier for this option.
     * 
     * @return The sound identifier string
     */
    public String getSoundId() {
        return this.soundId;
    }

    /**
     * Gets the display name for this option.
     * 
     * @return The human-readable display name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Converts this option to a Text object for display in the UI.
     * 
     * @return A Text object with the display name
     */
    public Text toText() {
        return Text.literal(this.displayName);
    }
}
