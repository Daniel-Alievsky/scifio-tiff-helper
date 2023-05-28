/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import org.scijava.Context;

import java.io.File;
import java.io.IOException;

public class TiffWriterTest {
    private static final int MAX_IMAGE_DIM = 5000;
    private static final int START_X = 0;
    private static final int START_Y = 0;

    public static void main(String[] args) throws IOException, FormatException {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("    " + TiffWriterTest.class.getName()
                    + " source.tif target.tif");
            return;
        }
        final File sourceFile = new File(args[0]);
        final File targetFile = new File(args[1]);

        System.out.printf("Opening %s...%n", sourceFile);

        final SCIFIO scifio = new SCIFIO();
        Context context = scifio.getContext();
        CachingTiffReader reader = new CachingTiffReader(context, sourceFile);
        SequentialTiffWriter writer = new SequentialTiffWriter(context, targetFile, false, true);
        System.out.printf("Writing %s...%n", targetFile);
        final int ifdCount = reader.getIFDCount();
        for (int ifdIndex = 0; ifdIndex < ifdCount; ifdIndex++) {
            final IFD ifd = reader.getIFDByIndex(ifdIndex);
            System.out.printf("Copying #%d/%d:%n%s", ifdIndex, ifdCount, TiffTools.toString(ifd));
            final int w = (int) Math.min(ifd.getImageWidth(), MAX_IMAGE_DIM);
            final int h = (int) Math.min(ifd.getImageLength(), MAX_IMAGE_DIM);
            final int bandCount = ifd.getSamplesPerPixel();
            byte[] bytes = reader.readSamples(null, null, ifdIndex, START_X, START_Y, w, h);
            bytes = TiffTools.interleaveSamples(bytes, w * h, ifd);
            final IFD newIfd = new IFD(null);
            writer.setInterleaved(true);
            writer.setCompression(ifd.getCompression());
            writer.setImageSizes(w, h);
            if (ifd.isTiled()) {
                writer.setTileSizes((int) ifd.getTileWidth(), (int) ifd.getTileLength());
                writer.setTiling(true);
            }
            writer.writeSeveralTilesOrStrips(bytes, newIfd, ifd.getPixelType(), bandCount,
                    START_X, START_Y, w, h, true, ifdIndex == ifdCount - 1);
        }
        writer.close();

        System.out.println("Done");
        context.dispose();
    }
}