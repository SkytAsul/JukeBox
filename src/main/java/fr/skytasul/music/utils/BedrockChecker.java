package fr.skytasul.music.utils;

import fr.skytasul.music.JukeBox;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.UUID;

public class BedrockChecker {
    
    /**
     * Check if player is a Bedrock Edition player
     * @param player Player to check
     * @return true if Bedrock Edition player, false otherwise
     */
    public static boolean isBedrockPlayer(Player player) {
        // First check if Floodgate is available
        if (!JukeBox.isFloodgateAvailable()) {
            return false;
        }
        
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            UUID uuid = player.getUniqueId();
            return api.isFloodgatePlayer(uuid);
        } catch (Exception e) {
            // Floodgate not installed or error occurred
            return false;
        }
    }
    
    /**
     * Get Bedrock Edition player info
     * @param player Player object
     * @return FloodgatePlayer object, or null if not a Bedrock Edition player
     */
    public static FloodgatePlayer getFloodgatePlayer(Player player) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            UUID uuid = player.getUniqueId();
            if (api.isFloodgatePlayer(uuid)) {
                return api.getPlayer(uuid);
            }
        } catch (Exception e) {
            // Floodgate not installed or error occurred
        }
        return null;
    }
}
