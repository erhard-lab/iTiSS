package gedi.utils;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.EI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenomicUtils {
    public static MemoryIntervalTreeStorage<String> getOverlappingGenes(Genomic genomic) {
        MemoryIntervalTreeStorage<String> genes = genomic.getGenes();
        Map<ReferenceSequence, List<ImmutableReferenceGenomicRegion<String>>> overlappingGenes = new HashMap<>();
        genes.ei().forEachRemaining(g -> {
            List<ImmutableReferenceGenomicRegion<String>> refGenes = overlappingGenes.computeIfAbsent(g.getReference(), absent -> new ArrayList<>());
            for (ImmutableReferenceGenomicRegion<String> gene : refGenes) {
                if (gene.getRegion().intersects(g.getRegion())) {
                    refGenes.remove(gene);
                    refGenes.add(g.toMutable().setRegion(g.getRegion().union(gene.getRegion())).setData(gene.getData() + "_" + g.getData()).toImmutable());
                    return;
                }
            }
            refGenes.add(g);
        });
        MemoryIntervalTreeStorage<String> out = new MemoryIntervalTreeStorage<>(String.class);
        out.fill(EI.wrap(overlappingGenes.values()).unfold(EI::wrap));
        return out;
    }

    public static MemoryIntervalTreeStorage<String> getUniqueTss(Genomic genomic, int mergingWindow) {
        MemoryIntervalTreeStorage<String> genes = getOverlappingGenes(genomic);
        MemoryIntervalTreeStorage<Transcript> transcripts = genomic.getTranscripts();
        MemoryIntervalTreeStorage<String> out = new MemoryIntervalTreeStorage<>(String.class);

        out.fill(genes.ei().unfold(g -> {
            ArrayList<ImmutableReferenceGenomicRegion<String>> extendedTssLst = transcripts.ei(g).map(m -> {
                int tss = GenomicRegionPosition.FivePrime.position(m);
                return new ImmutableReferenceGenomicRegion<>(m.getReference(), new ArrayGenomicRegion(tss - mergingWindow, tss + mergingWindow + 1), g.getData());
            }).list();
            ImmutableReferenceGenomicRegion<String> last = extendedTssLst.get(0);
            ArrayList<ImmutableReferenceGenomicRegion<String>> mergedTssLst = new ArrayList<>();
            for (int i = 1; i < extendedTssLst.size(); i++) {
                if (last.getRegion().intersects(extendedTssLst.get(i).getRegion())) {
                    last = new ImmutableReferenceGenomicRegion<>(last.getReference(), new ArrayGenomicRegion(last.getRegion().getStart(), extendedTssLst.get(i).getRegion().getEnd()), last.getData());
                } else {
                    mergedTssLst.add(last);
                    last = extendedTssLst.get(i);
                }
            }
            mergedTssLst.add(last);
            return EI.wrap(mergedTssLst);
        }));
        return out;
    }

    public static String getGeneNameFromGeneId(String geneId, Genomic genomic) {
        return genomic.getGeneTable("symbol").apply(geneId);
    }

    public static ImmutableReferenceGenomicRegion<NameAnnotation> getRgrFromGeneName(String geneName, Genomic genomic) {
        ReferenceGenomicRegion<?> rgr = genomic.getNameIndex().get(geneName);
        if (rgr == null) {
            return null;
        }
        return new ImmutableReferenceGenomicRegion<>(rgr.getReference(), rgr.getRegion(), new NameAnnotation(rgr.getData().toString()));
    }

    public static String toChromosomeAddedName(ReferenceSequence ref) {
        try {
            int i = Integer.parseInt(ref.getName());
            return "chr" + ref.getName();
        } catch (NumberFormatException e) {
            if (ref.getName().equals("X")) {
                return "chrX";
            }
            if (ref.getName().equals("Y")) {
                return "chrY";
            }
            return ref.getName();
        }
    }

    public static boolean isStandardChromosome(ReferenceSequence ref) {
        try {
            int i = Integer.parseInt(ref.getName());
            return true;
        } catch (NumberFormatException e) {
            if (ref.getName().equals("X")) {
                return true;
            }
            if (ref.getName().equals("Y")) {
                return true;
            }
            return false;
        }
    }
}
