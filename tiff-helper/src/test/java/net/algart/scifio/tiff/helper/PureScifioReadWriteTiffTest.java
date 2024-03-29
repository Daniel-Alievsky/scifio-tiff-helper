/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.scifio.tiff.helper;

import io.scif.FormatException;
import io.scif.SCIFIO;
import io.scif.formats.tiff.IFD;
import io.scif.formats.tiff.IFDList;
import io.scif.formats.tiff.TiffParser;
import io.scif.formats.tiff.TiffSaver;
import io.scif.util.FormatTools;
import org.scijava.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class PureScifioReadWriteTiffTest {
    private static final int MAX_IMAGE_DIM = 5000;

    public static void main(String[] args) throws IOException, FormatException {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("    " + PureScifioReadWriteTiffTest.class.getName()
                    + " source.tif target.tif");
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path targetFile = Paths.get(args[1]);

        System.out.printf("Opening %s...%n", sourceFile);

        final SCIFIO scifio = new SCIFIO();
        Context context = scifio.getContext();
        TiffParser reader = new TiffParser(context, sourceFile.toString());
        Files.deleteIfExists(targetFile);
        // - strange, but necessary
        TiffSaver saver = new TiffSaver(context, targetFile.toString());
        saver.setWritingSequentially(true);
        saver.setLittleEndian(true);
        saver.writeHeader();
        System.out.printf("Writing %s...%n", targetFile);
        final IFDList ifdList = reader.getIFDs();
        for (int ifdIndex = 0; ifdIndex < ifdList.size(); ifdIndex++) {
            final IFD ifd = ifdList.get(ifdIndex);
            System.out.printf("Copying #%d/%d:%n%s%n", ifdIndex, ifdList.size(), ifd);
            final int w = (int) Math.min(ifd.getImageWidth(), MAX_IMAGE_DIM);
            final int h = (int) Math.min(ifd.getImageLength(), MAX_IMAGE_DIM);
            Objects.requireNonNull(ifd, "Null IFD");
            byte[] bytes = new byte[w * h
                    * ifd.getSamplesPerPixel() * FormatTools.getBytesPerPixel(ifd.getPixelType())];
            reader.getSamples(ifd, bytes, 0, 0, w, h);
//                ExtendedTiffParser.correctUnusualPrecisions(ifd, bytes, w * h);
//                bytes = ExtendedTiffParser.interleaveSamples(ifd, bytes, w * h);
            boolean last = ifdIndex == ifdList.size() - 1;
            final IFD newIfd = new IFD(ifd, null);

            saver.writeImage(bytes, newIfd, -1, ifd.getPixelType(), 0, 0, w, h, last);
//                saver.getStream().seek(saver.getStream().length());

            System.out.printf("Effective IFD:%n%s%n", newIfd);
            // - does not write prefix
        }
        saver.getStream().close();
        context.dispose();
        System.out.println("Done");
    }

}
