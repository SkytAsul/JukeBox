package fr.skytasul.music.forms;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import fr.skytasul.music.JukeBox;
import fr.skytasul.music.PlayerData;
import fr.skytasul.music.utils.Lang;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.impl.CustomFormResponseImpl;
import org.geysermc.cumulus.response.impl.ModalFormResponseImpl;
import org.geysermc.cumulus.response.impl.SimpleFormResponseImpl;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.List;

public class BedrockMusicForm {
    
    private final Player player;
    private final PlayerData pdata;
    
    public BedrockMusicForm(Player player, PlayerData pdata) {
        this.player = player;
        this.pdata = pdata;
    }
    
    /**
     * Send form to Bedrock Edition player
     */
    private void sendForm(Object form) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
            if (floodgatePlayer != null) {
                // 修复：直接传递表单对象，不需要强制转换为 Object
                if (form instanceof SimpleForm) {
                    floodgatePlayer.sendForm((SimpleForm) form);
                } else if (form instanceof ModalForm) {
                    floodgatePlayer.sendForm((ModalForm) form);
                } else if (form instanceof CustomForm) {
                    floodgatePlayer.sendForm((CustomForm) form);
                }
            }
        } catch (Exception e) {
            player.sendMessage(Lang.BEDROCK_ERROR_FORM_DISPLAY + e.getMessage());
        }
    }
    
    /**
     * Display main menu form
     */
    public void showMainMenu() {
        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(Lang.BEDROCK_FORM_TITLE)
                .content(Lang.BEDROCK_FORM_CONTENT);
        
        // Add music list option
        if (!JukeBox.getSongs().isEmpty()) {
            formBuilder.button(Lang.BEDROCK_BUTTON_SONG_LIST, FormImage.Type.PATH, "textures/ui/copy.png");
        }
        
        // Add playback options (unified playback control)
        formBuilder.button(Lang.BEDROCK_BUTTON_PLAYBACK_OPTIONS, FormImage.Type.PATH, "textures/ui/video_glyph_color_2x.png");
        
        // Add smart radio toggle button
        String radioButtonText = pdata.getPlaylistType() == fr.skytasul.music.utils.Playlists.RADIO ? 
            Lang.BEDROCK_BUTTON_RADIO_OFF : Lang.BEDROCK_BUTTON_RADIO_ON;
        formBuilder.button(radioButtonText, FormImage.Type.PATH, "textures/blocks/reactor_core_stage_1.png");
        
        // Add settings option
        formBuilder.button(Lang.BEDROCK_BUTTON_SETTINGS, FormImage.Type.PATH, "textures/ui/settings_glyph_color_2x.png");
        
        formBuilder.validResultHandler(response -> {
            // 修复：正确处理响应
            if (response instanceof SimpleFormResponseImpl) {
                SimpleFormResponseImpl simpleResponse = (SimpleFormResponseImpl) response;
                int clickedButton = simpleResponse.clickedButtonId();
                handleMainMenuClick(clickedButton);
            }
        });
        
        sendForm(formBuilder.build());
    }
    
    /**
     * Handle main menu click event
     */
    private void handleMainMenuClick(int buttonId) {
        int currentIndex = 0;
        
        // Music list button
        if (!JukeBox.getSongs().isEmpty() && buttonId == currentIndex++) {
            showSongSelection();
            return;
        }
        
        // Playback options button
        if (buttonId == currentIndex++) {
            showPlaybackOptionsMenu();
            return;
        }

        // Smart radio button
        if (buttonId == currentIndex++) {
            if (pdata.getPlaylistType() == fr.skytasul.music.utils.Playlists.RADIO) {
                pdata.stopPlaying(true);
            } else {
                pdata.setPlaylist(fr.skytasul.music.utils.Playlists.RADIO, true);
            }
            showMainMenu();
            return;
        }
        
        // Settings button
        if (buttonId == currentIndex) {
            showSettingsMenu();
        }
    }
    
    /**
     * Display playback options submenu
     */
    private void showPlaybackOptionsMenu() {
        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(Lang.BEDROCK_PLAYBACK_TITLE)
                .content(Lang.BEDROCK_PLAYBACK_CONTENT);
        
        // Random play
        if (player.hasPermission("music.random")) {
            formBuilder.button(Lang.BEDROCK_BUTTON_RANDOM, FormImage.Type.PATH, "textures/ui/refresh_light.png");
        }
        
        // Pause/Resume
        if (pdata.isListening()) {
            formBuilder.button(Lang.BEDROCK_BUTTON_PAUSE_RESUME, FormImage.Type.PATH, "textures/items/end_crystal.png");
        }
        
        // Stop music
        formBuilder.button(Lang.BEDROCK_BUTTON_STOP, FormImage.Type.PATH, "textures/blocks/barrier.png");
        
        // Return to main menu
        formBuilder.button(Lang.BEDROCK_BUTTON_BACK, FormImage.Type.PATH, "textures/ui/wysiwyg_reset.png");
        
        formBuilder.validResultHandler(response -> {
            if (response instanceof SimpleFormResponseImpl) {
                SimpleFormResponseImpl simpleResponse = (SimpleFormResponseImpl) response;
                int clickedButton = simpleResponse.clickedButtonId();
                
                int currentIndex = 0;
                
                // Random play
                if (player.hasPermission("music.random")) {
                    if (clickedButton == currentIndex++) {
                        pdata.playRandom();
                        showPlaybackOptionsMenu(); // Re-display playback options menu
                        return;
                    }
                }
                
                // Pause/Resume
                if (pdata.isListening()) {
                    if (clickedButton == currentIndex++) {
                        pdata.togglePlaying();
                        showPlaybackOptionsMenu(); // Re-display playback options menu
                        return;
                    }
                }
                
                // Stop music
                if (clickedButton == currentIndex++) {
                    pdata.stopPlaying(true);
                    showPlaybackOptionsMenu(); // Re-display playback options menu
                    return;
                }
                
                // Return to main menu
                if (clickedButton == currentIndex) {
                    showMainMenu();
                    return;
                }
            }
            
            // Default return to main menu
            showMainMenu();
        });
        
        sendForm(formBuilder.build());
    }
    
    /**
     * Display song selection form
     */
    /**
     * Display song selection form (paginated version)
     */
    private void showSongSelection() {
        showSongSelectionPage(0); // Display first page by default
    }

    /**
     * Display song selection form for specified page
     * @param page Page number (starting from 0)
     */
    private void showSongSelectionPage(int page) {
        final int SONGS_PER_PAGE = 10; // Number of songs displayed per page
        List<Song> songs = JukeBox.getSongs();
        int totalPages = (int) Math.ceil((double) songs.size() / SONGS_PER_PAGE);

        // Ensure page number is within valid range
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(String.format(Lang.BEDROCK_SONG_SELECT_TITLE, page + 1, Math.max(totalPages, 1)))
                .content(Lang.BEDROCK_SONG_SELECT_CONTENT);

        // Calculate current page's song range
        int startIndex = page * SONGS_PER_PAGE;
        int endIndex = Math.min(startIndex + SONGS_PER_PAGE, songs.size());

        // Add current page's song buttons
        for (int i = startIndex; i < endIndex; i++) {
            Song song = songs.get(i);
            String songName = JukeBox.getSongName(song);
            String description = song.getDescription() != null ? song.getDescription() : "";
            String buttonText = "§b" + songName + "\n§r" + description;
            formBuilder.button(buttonText);
        }
        // Previous page button (if not first page)
        if (page > 0) {
            formBuilder.button(Lang.BEDROCK_BUTTON_PREV_PAGE, FormImage.Type.PATH, "textures/items/arrow.png");
        }

        // Next page button (if not last page)
        if (page < totalPages - 1) {
            formBuilder.button(Lang.BEDROCK_BUTTON_NEXT_PAGE, FormImage.Type.PATH, "textures/items/arrow.png");
        }
        // Add pagination control buttons
        formBuilder.button(Lang.BEDROCK_BUTTON_BACK, FormImage.Type.PATH, "textures/ui/wysiwyg_reset.png");


        int finalPage = page;
        formBuilder.validResultHandler(response -> {
            // 修复：正确处理响应
            if (response instanceof SimpleFormResponseImpl) {
                SimpleFormResponseImpl simpleResponse = (SimpleFormResponseImpl) response;
                int clickedButton = simpleResponse.clickedButtonId();

                // Calculate indices of various buttons
                int songButtonsCount = endIndex - startIndex;
                int prevButtonIndex = -1;
                int nextButtonIndex = -1;
                int returnButtonIndex = songButtonsCount;
                
                // If there is previous page button
                if (finalPage > 0) {
                    prevButtonIndex = songButtonsCount;
                    returnButtonIndex++; // Move return button index back by one position
                }
                
                // If there is next page button
                if (finalPage < totalPages - 1) {
                    if (finalPage > 0) {
                        nextButtonIndex = songButtonsCount + 1; // When there is previous page, next page is second control button
                        returnButtonIndex++; // Move return button index back by one more position
                    } else {
                        nextButtonIndex = songButtonsCount; // When there is no previous page, next page is first control button
                        returnButtonIndex++; // Move return button index back by one position
                    }
                }
                
                if (clickedButton < songButtonsCount) {
                    // Clicked song button
                    int songIndex = startIndex + clickedButton;
                    if (songIndex < songs.size()) {
                        Song selectedSong = songs.get(songIndex);
                        pdata.playSong(selectedSong);
                        showMainMenu(); // Return to main menu after playing
                        return;
                    }
                } else if (clickedButton == prevButtonIndex && finalPage > 0) {
                    // Clicked previous page button
                    showSongSelectionPage(finalPage - 1);
                    return;
                } else if (clickedButton == nextButtonIndex && finalPage < totalPages - 1) {
                    // Clicked next page button
                    showSongSelectionPage(finalPage + 1);
                    return;
                } else if (clickedButton == returnButtonIndex) {
                    // Clicked return to main menu button
                    showMainMenu();
                    return;
                }
            }
            showMainMenu();
        });

        sendForm(formBuilder.build());
    }

    
    /**
     * Display playlist menu
     */
    private void showPlaylistMenu() {
        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(Lang.BEDROCK_PLAYLIST_TITLE)
                .content(Lang.BEDROCK_PLAYLIST_CONTENT);
        
        // Display current playlist type
        String currentPlaylist = pdata.getPlaylistType().name();
        formBuilder.content(Lang.BEDROCK_PLAYLIST_CURRENT + currentPlaylist);
        
        // Switch playlist
        formBuilder.button(Lang.BEDROCK_BUTTON_SWITCH_PLAYLIST, FormImage.Type.PATH, "textures/items/player_head.png");
        
        // Next song
        formBuilder.button(Lang.BEDROCK_BUTTON_NEXT_SONG, FormImage.Type.PATH, "textures/items/feather.png");
        
        // Clear playlist
        formBuilder.button(Lang.BEDROCK_BUTTON_CLEAR_PLAYLIST, FormImage.Type.PATH, "textures/blocks/lava.png");
        
        formBuilder.button(Lang.BEDROCK_BUTTON_BACK, FormImage.Type.PATH, "textures/ui/wysiwyg_reset.png");
        
        formBuilder.validResultHandler(response -> {
            // 修复：正确处理响应
            if (response instanceof SimpleFormResponseImpl) {
                SimpleFormResponseImpl simpleResponse = (SimpleFormResponseImpl) response;
                int clickedButton = simpleResponse.clickedButtonId();
                
                switch (clickedButton) {
                    case 0: // Switch playlist
                        pdata.nextPlaylist();
                        showPlaylistMenu();
                        break;
                    case 1: // Next song
                        pdata.nextSong();
                        showPlaylistMenu();
                        break;
                    case 2: // Clear playlist
                        showClearPlaylistConfirmation();
                        break;
                    case 3: // Return to main menu
                        showMainMenu();
                        break;
                }
            }
        });
        
        sendForm(formBuilder.build());
    }
    
    /**
     * Display clear playlist confirmation dialog
     */
    private void showClearPlaylistConfirmation() {
        ModalForm.Builder formBuilder = ModalForm.builder()
                .title(Lang.BEDROCK_CLEAR_CONFIRM_TITLE)
                .content(Lang.BEDROCK_CLEAR_CONFIRM_CONTENT)
                .button1(Lang.BEDROCK_BUTTON_CONFIRM)
                .button2(Lang.BEDROCK_BUTTON_CANCEL);
        
        formBuilder.validResultHandler(response -> {
            // 修复：正确处理响应
            if (response instanceof ModalFormResponseImpl) {
                ModalFormResponseImpl modalResponse = (ModalFormResponseImpl) response;
                if (modalResponse.clickedButtonId() == 0) { // Confirm
                    pdata.clearPlaylist();
                    player.sendMessage(Lang.BEDROCK_PLAYLIST_CLEARED);
                }
            }
            showPlaylistMenu();
        });
        
        sendForm(formBuilder.build());
    }
    
    /**
     * Display settings menu
     */
    private void showSettingsMenu() {
        CustomForm.Builder formBuilder = CustomForm.builder()
                .title(Lang.BEDROCK_SETTINGS_TITLE);
        
        // Volume slider (0-100)
        formBuilder.slider(Lang.BEDROCK_SETTINGS_VOLUME, 0, 100, 1, pdata.getVolume());
        
        // Various toggle options
        if (JukeBox.particles && player.hasPermission("music.particles")) {
            formBuilder.toggle(Lang.BEDROCK_SETTINGS_PARTICLES, pdata.hasParticles());
        }
        
        if (player.hasPermission("music.play-on-join")) {
            formBuilder.toggle(Lang.BEDROCK_SETTINGS_JOIN_MUSIC, pdata.hasJoinMusic());
        }
        
        if (player.hasPermission("music.shuffle")) {
            formBuilder.toggle(Lang.BEDROCK_SETTINGS_SHUFFLE, pdata.isShuffle());
        }
        
        if (player.hasPermission("music.loop")) {
            formBuilder.toggle(Lang.BEDROCK_SETTINGS_LOOP, pdata.isRepeatEnabled());
        }
        
        formBuilder.validResultHandler(response -> {
            // 修复：正确处理响应
            if (response instanceof CustomFormResponseImpl) {
                CustomFormResponseImpl customResponse = (CustomFormResponseImpl) response;
                
                int fieldIndex = 0;
                
                // Volume setting (index 0)
                int volume = (int) customResponse.asSlider(fieldIndex++);
                pdata.setVolume(volume);
                
                // Particle effects
                if (JukeBox.particles && player.hasPermission("music.particles")) {
                    boolean particles = customResponse.asToggle(fieldIndex++);
                    pdata.setParticles(particles);
                }
                
                // Play music on join
                if (player.hasPermission("music.play-on-join")) {
                    boolean joinMusic = customResponse.asToggle(fieldIndex++);
                    pdata.setJoinMusic(joinMusic);
                }
                
                // Shuffle mode
                if (player.hasPermission("music.shuffle")) {
                    boolean shuffle = customResponse.asToggle(fieldIndex++);
                    pdata.setShuffle(shuffle);
                }
                
                // Loop playback
                if (player.hasPermission("music.loop")) {
                    boolean repeat = customResponse.asToggle(fieldIndex++);
                    pdata.setRepeat(repeat);
                }
                
                // Return to main menu after saving
                player.sendMessage(Lang.BEDROCK_SETTINGS_SAVED);
                showMainMenu();
            } else {
                showMainMenu(); // If no valid response, also return to main menu
            }
        });
        
        sendForm(formBuilder.build());
    }
}
