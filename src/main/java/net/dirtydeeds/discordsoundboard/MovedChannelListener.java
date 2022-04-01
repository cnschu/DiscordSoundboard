package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.User;
import net.dirtydeeds.discordsoundboard.repository.UserRepository;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.h2.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovedChannelListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MovedChannelListener.class);

    private final SoundPlayerImpl bot;
    private final UserRepository userRepository;
    private final boolean playEntranceOnMove;

    public MovedChannelListener(SoundPlayerImpl bot, UserRepository userRepository,
                                boolean playEntranceOnMove) {
        this.bot = bot;
        this.userRepository = userRepository;
        this.playEntranceOnMove = playEntranceOnMove;
    }

    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        if (playEntranceOnMove && !event.getMember().getUser().isBot()) {
            String discordUser = event.getMember().getEffectiveName();
            String discordUserId = event.getMember().getId();
            String entranceFile = bot.getFileForUser(discordUser, true);
            String disconnectFile = bot.getFileForUser(discordUser, false);

            User user = userRepository.findOneByIdOrUsernameIgnoreCase(discordUserId, discordUser);
            if (user != null) {
                if (!StringUtils.isNullOrEmpty(user.getEntranceSound())) {
                    entranceFile = user.getEntranceSound();
                    LOG.info("Playing move sound {}", entranceFile);
                }
                if (!StringUtils.isNullOrEmpty(user.getLeaveSound())) {
                    disconnectFile = user.getLeaveSound();
                    LOG.info("Playing leave sound {}", disconnectFile);
                }
            }
            if (!StringUtils.isNullOrEmpty(bot.entranceForAll)) {
                entranceFile = bot.entranceForAll;
                LOG.info("Playing entrance for all sound {}", entranceFile);
            }

            if (!entranceFile.equals("")) {
                try {
                    bot.playFileInChannel(entranceFile, event.getChannelJoined());
                } catch (Exception e) {
                    LOG.error("Could not play file for entrance of {}", user);
                }
            } else if (!disconnectFile.equals("")) {
                try {
                    bot.playFileInChannel(disconnectFile, event.getChannelLeft());
                } catch (Exception e) {
                    LOG.error("Could not play file for disconnection of {}", user);
                }
            } else {
                LOG.debug("Could not find entrance or disconnect sound for {}, so ignoring GuildVoiceMoveEvent.", user);
            }
        }
    }
}
