/*
 * Copyright (c) 2016, garantiertnicht
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by garantiertnicht Weichware.
 * 4. Neither the name of garantiertnicht Weichware nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY GARANTIERTNICHT WEICHWARE ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GARANTIERTNICHT WEICHWARE BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.garantiertnicht.Weichware;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {

    public static void main(String... args) throws IOException {
        if(args.length != 6) {
            System.exit(3);
        }

        patchJar(args[0], args[1], args[2], args[3], args[4], Boolean.valueOf(args[5]));
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        final byte[] BUFFER = new byte[4096 * 1024];
        while ((bytesRead = input.read(BUFFER))!= -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }

    public static void patchJar(String mcDir, String assetsUrl, String jarName, String modId, String ver, boolean gui) throws IOException {
        FileSystem fs = FileSystems.getDefault();

        if(Files.exists(fs.getPath(mcDir + File.separator + "gwwAssetsLoader-NeverShowGUI")))
            gui = false;

        URL website;
        ReadableByteChannel rbc;

        String uPath = mcDir + File.separator + "mods" + File.separator + jarName + "-update.zip";

        Path updateP = fs.getPath(uPath);
        website = new URL(assetsUrl);
        rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(uPath);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();

        ZipFile main = new ZipFile(mcDir + File.separator + "mods" + File.separator + jarName + ".jar");
        ZipFile update = new ZipFile(uPath);
        ZipOutputStream newZip = new ZipOutputStream(new FileOutputStream(mcDir + File.separator + "mods" + File.separator + jarName + ".jar_temp"));

        Enumeration<? extends ZipEntry> entries = main.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();

            if(e.getName().startsWith("assets"))
                continue;

            ZipEntry copy = new ZipEntry(e.getName());

            newZip.putNextEntry(copy);
            if (!e.isDirectory()) {
                copy(main.getInputStream(copy), newZip);
            }

            System.out.println(e.getName());

            newZip.closeEntry();
        }

        entries = update.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if(!e.getName().startsWith("assets")) {
                continue;
            }

            ZipEntry copy = new ZipEntry(e.getName());

            newZip.putNextEntry(copy);
            if (!e.isDirectory()) {
                copy(update.getInputStream(e), newZip);
            }

            System.out.println(e.getName());

            newZip.closeEntry();
        }

        ZipEntry e = new ZipEntry("assets/" + modId + "/version");
        newZip.putNextEntry(e);

        newZip.write(ver.getBytes());
        newZip.closeEntry();

        // close
        main.close();
        newZip.close();
        update.close();

        boolean success = false;
        for(int i = 0; i < 15; i++) {
            try {
                Path old = fs.getPath(mcDir + File.separator + "mods" + File.separator + jarName + ".jar");
                Path temp = fs.getPath(mcDir + File.separator + "mods" + File.separator + jarName + ".jar_temp");

                Files.move(temp, old, StandardCopyOption.REPLACE_EXISTING);

                success = true;
                break;
            } catch (java.nio.file.FileSystemException exc) {
                exc.printStackTrace();
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException e1) {
                    System.exit(2);
                }
            }
        }

        if(!success) {
            if(gui)
                JOptionPane.showMessageDialog(null, String.format("ImperiumBlocks: Update failed! Please move %s.jar_temp to %s.jar in your mods folder." +
                        "mods folder. ~Imperium 1871", jarName, jarName));
            System.exit(4);
        }

        Files.deleteIfExists(updateP);

        if(gui)
            JOptionPane.showMessageDialog(null, "ImperiumBlocks: An update for our blocks got applied. Please restart the game.");
    }
}
