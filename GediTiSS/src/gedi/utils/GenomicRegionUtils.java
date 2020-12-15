package gedi.utils;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

public class GenomicRegionUtils {
    public static boolean isContainedInIntron(GenomicRegion region, int pos) {
        return pos >= region.getStart() && pos < region.getEnd() && !region.contains(pos);
    }

    public static boolean notInIntron(GenomicRegion region, int pos) {
        return region.contains(pos) || pos < region.getStart() || pos >= region.getEnd();
    }

    public static int getIntronIndex(GenomicRegion region, int pos) {
        if (!isContainedInIntron(region, pos)) {
            return -1;
        }
        return getPartIndes(region.invert(), pos);
    }

    public static int getPartIndes(GenomicRegion region, int pos) {
        if (!region.contains(pos)) {
            return -1;
        }
        for (int i = 0; i < region.getNumParts(); i++) {
            if (region.getPart(i).asRegion().contains(pos)) {
                return i;
            }
        }
        return -1;
    }

    public static GenomicRegion removeIntron(GenomicRegion region, int intronIndex) {
        if (intronIndex < 0 || intronIndex > region.invert().getNumParts()) {
            throw new IndexOutOfBoundsException("Region intron parts: " + region.invert().getNumParts() + ", index: " + intronIndex);
        }

        int[] newRegionIndices = new int[(region.getNumParts()-1)*2];
        int newRegionIndex = 0;
        for (int i = 0; i < region.getNumParts(); i++) {
            if (i == intronIndex) {
                newRegionIndices[newRegionIndex*2] = region.getPart(i).getStart();
                newRegionIndices[newRegionIndex*2+1] = region.getPart(i+1).getEnd();
                newRegionIndex++;
                i++;
                continue;
            }
            newRegionIndices[newRegionIndex*2] = region.getPart(i).getStart();
            newRegionIndices[newRegionIndex*2+1] = region.getPart(i).getEnd();
            newRegionIndex++;
        }

        return new ArrayGenomicRegion(newRegionIndices);
    }

    public static GenomicRegion createRegion(ReferenceSequence ref, int tss, int tts) {
        if (ref.isPlus()) {
            return new ArrayGenomicRegion(tss, tts + 1);
        } else {
            return new ArrayGenomicRegion(tts, tss + 1);
        }
    }
}
