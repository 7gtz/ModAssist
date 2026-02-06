package sevengtz.autochatmod.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.SoundOption;

/**
 * Utility class for playing sounds in the game.
 * Provides a centralized way to play alert sounds with proper error handling.
 */
public final class SoundUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");

    private SoundUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Plays a sound from the given SoundOption.
     * 
     * @param sound  The sound option to play
     * @param volume Volume level (0.0 to 2.0)
     * @param pitch  Pitch level (0.1 to 2.0)
     */
    public static void playSound(SoundOption sound, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        try {
            String soundIdString = sound.getSoundId();
            Identifier soundId = Identifier.of(soundIdString);
            SoundEvent soundEvent = SoundEvent.of(soundId);

            PositionedSoundInstance soundInstance = new PositionedSoundInstance(
                    soundEvent,
                    SoundCategory.PLAYERS,
                    volume,
                    pitch,
                    Random.create(),
                    client.player.getBlockPos());

            client.getSoundManager().play(soundInstance);
            LOGGER.debug("[AutoChatMod]: Played sound '{}'", soundIdString);

        } catch (Exception e) {
            LOGGER.error("[AutoChatMod]: Failed to play sound.", e);
        }
    }

    /**
     * Plays the default alert sound using configuration settings.
     * 
     * @param config The configuration to get sound settings from
     */
    public static void playAlertSound(sevengtz.autochatmod.config.ModConfig config) {
        if (!config.alertSoundEnabled)
            return;
        playSound(config.alertSound, config.alertSoundVolume, config.alertSoundPitch);
    }
}
