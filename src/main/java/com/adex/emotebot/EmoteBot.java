package com.adex.emotebot;

//import net.dv8tion.jda.api.JDA;
//import net.dv8tion.jda.api.JDABuilder;
//import net.dv8tion.jda.api.entities.Activity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EmoteBot {

     private final JDA jda;

    public EmoteBot(String token) throws LoginException, InterruptedException {
        DiscordListener discordListener = new DiscordListener();

        jda = JDABuilder.createDefault(token)
                .addEventListeners(discordListener)
                .setActivity(Activity.watching("Spinning Profiles"))
                .build()
                .awaitReady();

        discordListener.registerCommands(jda);
    }

    public static void main(String[] args) throws IOException {
        JsonObject configJson = getConfigJson();
        String token = configJson.get("token").getAsString();

        try {
            new EmoteBot(token);
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

        //String[] names = new String[]{"adex", "anostail", "blank", "bruhlicious", "dark", "dead", "dex", "fissy", "gray", "icy", "luigi", "mallory", "moon", "name", "oikawa", "rachy", "ricesicle", "shiro", "stormy", "veron"};
        //String[] names = new String[]{"duck1", "duck2", "duck3", "mamba", "townater", "zy", "sappy", "lenny", "miroslav", "greed", "bunni", "chain", "severus"};


        String[] names = new String[0];

        String outPath = "E:/discord/out/";
        String path = "E:/discord/";

        int steps = 64;
        int delay = 2;
        for (String name : names) {
            File file = new File(path + name + "/" + name + ".png");

            BufferedImage[] transparentFrames = new BufferedImage[steps];
            BufferedImage[] solidFrames = new BufferedImage[steps];
            BufferedImage base;
            try {
                BufferedImage image = ImageIO.read(file);
                base = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = base.createGraphics();
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
            } catch (IOException exception) {
                file = new File(path + name + "/" + name + ".gif");
                try {
                    base = ImageIO.read(file);
                } catch (IOException exception1) {
                    System.out.println("Invalid file: " + name);
                    continue;
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
                    solidFrames[i] = Editor.rotate(frame, degrees, false);
                }
                gif = GifSequenceWriter.createGif(new File(outPath + name + ".gif"), delay, transparentFrames);
                gif.createNewFile();

                gif = GifSequenceWriter.createGif(new File(outPath + name + "_bg.gif"), delay, solidFrames);
                gif.createNewFile();
                //size -= 5;
                //if (size < 10) {
                System.out.println(gif.length());
                /*if (gif.length() > 262144L) {
                    System.out.println("Too big image for " + name);
                    break;
                }
                 } while (gif.length() > 262144L);*/

            } catch (IOException exception) {
                System.out.println(exception.getMessage());
                System.out.println("Can't generate gif for " + name);
                continue;
            }

            for (int i = 0; i < solidFrames.length; i++) {
                BufferedImage solidFrame = solidFrames[i];
                File output = new File(outPath + name + i + ".png");
                ImageIO.write(solidFrame, "png", output);

                BufferedImage transparentFrame = solidFrames[i];
                File output1 = new File(outPath + name + i + "_bg.png");
                ImageIO.write(transparentFrame, "png", output1);
            }

            System.out.println(name);

        }
    }

    private static JsonObject getConfigJson() {
        String filePath = "config.json";

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Config json file not found!");
            System.exit(1);
            return new JsonObject();
        }

        JsonObject json;
        try {
            Reader reader = Files.newBufferedReader(Paths.get(filePath));
            json = new Gson().fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            System.out.println("Invalid config json!");
            System.exit(1);
            return new JsonObject();
        }

        return json;
    }
}
