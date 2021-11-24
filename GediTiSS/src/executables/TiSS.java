package executables;

import gedi.TiSSController;
import gedi.TiSSParameterSet;
import gedi.util.program.CommandLineHandler;
import gedi.util.program.GediProgram;
import gedi.utils.nonGediCompatible.ConvertBam;
import gedi.utils.nonGediCompatible.GenomicCreate;

public class TiSS {
    public static void main(String[] args) {
        TiSSParameterSet params = new TiSSParameterSet();

        GediProgram tiss = GediProgram.create("TiSS",
                new GenomicCreate(params),
                new ConvertBam(params),
                new TiSSController(params));

        GediProgram.run(tiss, params.paramFile, new CommandLineHandler("TiSS",
                "TiSS is used for TiSS-detection in TiSS-profiling datasets", args));
    }
}
