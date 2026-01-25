//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod;

import net.minecraft.text.Text;

public enum SoundOption {
    EXPERIENCE_ORB("Experience Orb", "minecraft:entity.experience_orb.pickup"),
    LEVEL_UP("Level Up", "minecraft:entity.player.levelup"),
    ITEM_PICKUP("Item Pickup", "minecraft:entity.item.pickup"),
    NOTE_PLING("Note Block 'Pling'", "minecraft:block.note_block.pling"),
    BELL_RING("Bell Ring", "minecraft:block.bell.use"),
    ANVIL_LAND("Anvil Land", "minecraft:block.anvil.land"),
    AMETHYST_CHIME("Amethyst Chime", "minecraft:block.amethyst_block.chime"),
    ENDER_CHEST("Ender Chest Close", "minecraft:block.ender_chest.close"),
    TRIPWIRE("Tripwire Click", "minecraft:block.tripwire.click_on"),
    GOAT_HORN("Goat Horn", "minecraft:item.goat_horn.sound.0"),
    VILLAGER_NO("Villager 'No'", "minecraft:entity.villager.no"),
    VILLAGER_YES("Villager 'Yes'", "minecraft:entity.villager.yes"),
    CAT_MEOW("Cat Meow", "minecraft:entity.cat.purr"),
    IRON_GOLEM_REPAIR("Golem Repair", "minecraft:entity.iron_golem.repair"),
    BLAZE_SHOOT("Blaze Shoot", "minecraft:entity.blaze.shoot"),
    ENDERMAN_TELEPORT("Enderman Teleport", "minecraft:entity.enderman.teleport"),
    GHAST_WARN("Ghast Warn", "minecraft:entity.ghast.warn"),
    GUARDIAN_ATTACK("Guardian Beam", "minecraft:entity.guardian.attack"),
    WITHER_SPAWN("Wither Spawn", "minecraft:entity.wither.spawn"),
    ENDER_DRAGON_GROWL("Dragon Growl", "minecraft:entity.ender_dragon.growl");

    private final String name;
    private final String soundId;

    private SoundOption(String name, String soundId) {
        this.name = name;
        this.soundId = soundId;
    }

    public String getSoundId() {
        return this.soundId;
    }

    public Text toText() {
        return Text.literal(this.name);
    }
}
