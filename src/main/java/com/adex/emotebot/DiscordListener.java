package com.adex.emotebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * This bot is coded on a bad way.
 * Since I don't feel like adding more commands or features,
 * I don't feel like it's worth to create a class for each command.
 * I put this on Git only to show people how to gifs are created, not to showcase the code.
 * Speaking of the code, the cool stuff is just copied from few places on online and are just combined to the bot by me.
 */
public class DiscordListener extends ListenerAdapter {

    public static final String GITHUB_LINK = "https://github.com/adex720/EmoteBot";

    private static final long ADEX = 560815341140181034L;

    private static final int steps = 16;
    private static final int delay = 80;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        //if (!event.isFromGuild()) return;

        String avatar = event.getInteraction().getUser().getAvatarUrl();
        String name = event.getInteraction().getUser().getName();

        OptionMapping option = event.getOption("user");

        if (option != null) {
            if (option.getType() == OptionType.USER) {
                User user = option.getAsUser();

                avatar = user.getAvatarUrl();
                name = user.getName();
            } else {
                name = option.getAsString();
            }
        }

        try {
            switch (event.getName()) {
                case "spin" -> createGif(event, avatar, name);
                case "frames" -> sendFrames(event, avatar, name);
                case "load" -> loadGif(event, name);
                case "github" -> sendGitHubLink(event);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }

    public void registerCommands(JDA jda) {
        Set<CommandData> commandData = new HashSet<>();

        commandData.add(new CommandDataImpl("spin", "Creates a spin emote.").setDefaultEnabled(true)
                .addOption(OptionType.USER, "user", "User whose avatar to use", false));

        commandData.add(new CommandDataImpl("frames", "Creates a spin emote frames.").setDefaultEnabled(true)
                .addOption(OptionType.USER, "user", "User whose avatar to use", false));

        commandData.add(new CommandDataImpl("load", "Loads existing spin emote.").setDefaultEnabled(true)
                .addOption(OptionType.STRING, "user", "User whose avatar to load", true));


        jda.updateCommands().addCommands(commandData).queue();
    }

    public static MessageEmbed createEmbed(String title, String fieldTitle, String description, Color color, @Nullable User author) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .addField(fieldTitle, description, false)
                .setColor(color);

        if (author != null) {
            builder.setFooter(author.getName(), author.getAvatarUrl())
                    .setTimestamp(new Date().toInstant());
        }

        return builder.build();
    }

    public static void createGif(SlashCommandInteractionEvent event, String avatar, String name) {

        String path = "E:/discord/cache/" + name + avatar.substring(avatar.length() - 4);
        File file = new File(path);

        try (BufferedInputStream in = new BufferedInputStream(new URL(avatar).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        BufferedImage[] transparentFrames = new BufferedImage[steps];
        BufferedImage base;
        try {
            BufferedImage image = ImageIO.read(file);
            base = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = base.createGraphics();
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
            g.drawRect(0, 0, 0, 0);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            g.dispose();
        } catch (IOException exception) {
            file = new File(avatar);
            try {
                base = ImageIO.read(file);

            } catch (IOException exception1) {
                event.reply(exception.getMessage()).queue();
                file.delete();
                return;
            }
        }

        File gif;
        try {
            int size = 100;
            //do {
            BufferedImage frame = Editor.makeSmaller(base, size);
            for (int i = 0; i < steps; i++) {
                int degrees = -Math.round(360f / steps * i);
                transparentFrames[i] = Editor.rotate(frame, degrees, true);
            }
            gif = GifSequenceWriter.createGif(new File("E:/discord/out/" + name + ".gif"), delay, transparentFrames);

            event.reply("Here's the gif!").addFile(gif).queue();

            if (gif.length() > 262144L) {
                if (event.getInteraction().getUser().getIdLong() != ADEX) {

                    event.getHook().sendMessage("The emote is too large to be a discord emote! You can manually combine the frames to a gif with `/frames`.").queue();
                    file.delete();
                    return;
                }

                for (int i = 0; i < transparentFrames.length; i++) {
                    BufferedImage transparentFrame = transparentFrames[i];
                    File output = new File("E:/discord/out/" + name + i + ".png");
                    ImageIO.write(transparentFrame, "png", output);
                }
                event.getHook().sendMessage("The emote is too large to be a discord emote! The frames are saved on your pc.").queue();
            }

        } catch (IOException exception) {
            event.getHook().sendMessage(exception.getMessage() + Arrays.toString(exception.getStackTrace())).queue();
        }
        file.delete();

    }

    public static void sendFrames(SlashCommandInteractionEvent event, String avatar, String name) {
        String path = "E:/discord/cache/" + name + avatar.substring(avatar.length() - 4);
        File file = new File(path);

        try (BufferedInputStream in = new BufferedInputStream(new URL(avatar).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        BufferedImage[] transparentFrames = new BufferedImage[steps];
        BufferedImage base;
        try {
            BufferedImage image = ImageIO.read(file);
            base = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = base.createGraphics();
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g.dispose();
        } catch (IOException exception) {
            file = new File(avatar);
            try {
                base = ImageIO.read(file);
            } catch (IOException exception1) {
                event.reply(exception.getMessage()).queue();
                file.delete();
                return;
            }
        }


        try {
            event.reply("Here are the frames").queue();

            int size = 100;
            //do {
            BufferedImage frame = Editor.makeSmaller(base, size);
            for (int i = 0; i < steps; i++) {
                int degrees = -Math.round(360f / steps * i);
                ImageIO.write(Editor.rotate(frame, degrees, true), "png", file);

                event.getHook().sendMessage("").addFile(file).complete();
            }

        } catch (IOException exception) {
            event.getHook().sendMessage(exception.getMessage() + Arrays.toString(exception.getStackTrace())).queue();
        }
        file.delete();
    }

    public static void loadGif(SlashCommandInteractionEvent event, String name) {

        if (event.getInteraction().getUser().getIdLong() != ADEX) {
            event.reply("You need to be Adex to use this command").queue();
            return;
        }

        File spin = new File("E:/discord/out/" + name + ".gif");
        if (spin.exists()) {
            event.reply("Spinning").addFile(spin).queue();
        } else {
            event.reply("Given gif does not exist").queue();
        }
    }

    public static void sendGitHubLink(SlashCommandInteractionEvent event) {
        event.reply("See the source code at " + GITHUB_LINK).queue();
    }

//    @Override
//    public void onGuildJoin(@NotNull GuildJoinEvent event) {
//        int steps = 16;
//        int delay = 80;
//
//        MessageChannel channel = event.getGuild().getDefaultChannel();
//        if (channel == null){
//            channel = event.getGuild().getSystemChannel();
//        }
//
//        for (Member member : event.getGuild().getMembers()) {
//            User user = member.getUser();
//
//            BufferedImage[] frames = new BufferedImage[steps];
//            BufferedImage base;
//            System.out.println(member.getNickname());
//            try {
//                base = ImageIO.read(new URL(user.getAvatarUrl()));
//            } catch (IOException | NullPointerException exception) {
//                exception.printStackTrace();
//               channel.sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't find user avatar", "Try again later", Color.RED, null)).queue();
//                return;
//            }
//
//            for (int i = 0; i < steps; i++) {
//                int degrees = Math.round(360f / steps * i);
//                frames[i] = Editor.rotate(base, degrees);
//            }
//
//            File gif;
//            try {
//                gif =GifSequenceWriter.createGif(new File("spin.gif"), delay, frames);
//            } catch (IOException exception) {
//                exception.printStackTrace();
//                channel.sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Something went wrong", "Try again later", Color.RED, null)).queue();
//                return;
//            }
//
//            event.getGuild().getDefaultChannel().sendMessage("There you go!").addFile(gif).queue();
//        }
//    }
}
