package com.adex.emotebot;
//
//import net.dv8tion.jda.api.entities.User;
//import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class GifSequenceWriter {

    protected ImageWriter writer;
    protected ImageWriteParam params;
    protected IIOMetadata metadata;

    public GifSequenceWriter(ImageOutputStream out, int imageType, int delay, boolean loop) throws IOException {
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);

        configureRootMetadata(delay, loop);

        writer.setOutput(out);
        writer.prepareWriteSequence(null);

    }

    private void configureRootMetadata(int delay, boolean loop) throws IIOInvalidTreeException {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "TRUE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by: https://memorynotfound.com");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loopContinuously = loop ? 0 : 1;
        child.setUserObject(new byte[]{0x1, (byte) (loopContinuously & 0xFF), (byte) ((loopContinuously >> 8) & 0xFF)});
        appExtensionsNode.appendChild(child);
        metadata.setFromTree(metaFormatName, root);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return (node);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        writer.writeToSequence(new IIOImage(img, null, metadata), params);
    }

    public void close() throws IOException {
        writer.endWriteSequence();
    }

    public static File createGif(File file, int delay, BufferedImage... images) throws IOException {
        if (images.length == 0) {
            throw new IllegalArgumentException("Give at least one image");
        }

        BufferedImage first = images[0];
        ImageOutputStream output = new FileImageOutputStream(file);

        GifSequenceWriter writer = new GifSequenceWriter(output, first.getType(), delay, true);
        writer.writeToSequence(first);

        boolean add = false;
        for (BufferedImage image : images) {
            if (add) {
                writer.writeToSequence(image);
            } else {
                add = true;
            }
        }

        writer.close();
        output.close();

        return file;
    }

    /*public static void createGif(GuildMessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Missing fields", "You need to present the steps and the delay", Color.RED, event.getAuthor())).queue();
            return;
        }

        int steps;
        int delay;
        User user;

        try {
            steps = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the steps", "`" + args[0] + "` is not a number", Color.RED, event.getAuthor())).queue();
            return;
        }

        if (steps < 3) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the steps", "Minimum value for steps is 3.", Color.RED, event.getAuthor())).queue();
            return;
        }
        if (steps > 32) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the steps", "Maximum value for steps is 32.", Color.RED, event.getAuthor())).queue();
            return;
        }

        try {
            delay = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the delay", "`" + args[1] + "` is not a number", Color.RED, event.getAuthor())).queue();
            return;
        }

        if (delay < 10) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the delay", "Minimum value for delay is 10.", Color.RED, event.getAuthor())).queue();
            return;
        }
        if (delay > 1000) {
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't set the delay", "Maximum value for delay is 1000.", Color.RED, event.getAuthor())).queue();
            return;
        }

        List<User> users = event.getMessage().getMentionedUsers();
        if (users.isEmpty()) {
            if (args.length > 2) {
                long id;
                try {
                    id = Long.parseLong(args[2]);
                } catch (NumberFormatException ignored) {
                    event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't select user", "`" + args[2] + "` is not a number", Color.RED, event.getAuthor())).queue();
                    return;
                }
                user = event.getJDA().getUserById(id);

                if (user == null) {
                    event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't find user", "`" + args[2] + "` can't be found", Color.RED, event.getAuthor())).queue();
                    return;
                }
            } else {
                user = event.getAuthor();
            }
        } else {
            user = users.get(0);
        }


        BufferedImage[] frames = new BufferedImage[steps];
        BufferedImage base;
        try {
            base = ImageIO.read(new URL(user.getAvatarUrl()));
        } catch (IOException exception) {
            exception.printStackTrace();
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Can't find user avatar", "Try again later", Color.RED, event.getAuthor())).queue();
            return;
        }

        for (int i = 0; i < steps; i++) {
            int degrees = Math.round(360f / steps * i);
            frames[i] = Editor.rotate(base, degrees);
        }

        File gif;
        try {
            gif = createGif(new File("spin.gif"), delay, frames);
        } catch (IOException exception) {
            exception.printStackTrace();
            event.getChannel().sendMessage(DiscordListener.createEmbed("CAN'T GENERATE GIF", "Something went wrong", "Try again later", Color.RED, event.getAuthor())).queue();
            return;
        }

        event.getChannel().sendMessage("There you go!").addFile(gif).queue();
    }/**/

}