/*
 * Decompiled with CFR 0_102.
 */
package util;

import javax.swing.SwingUtilities;

public abstract class SwingWorker {
    private Object value;
    private ThreadVar threadVar;

    protected synchronized Object getValue() {
        return this.value;
    }

    private synchronized void setValue(Object x) {
        this.value = x;
    }

    public abstract Object construct();

    public void finished() {
    }

    public void interrupt() {
        Thread t = this.threadVar.get();
        if (t != null) {
            t.interrupt();
        }
        this.threadVar.clear();
    }

    /*
     * Exception decompiling
     */
    public Object get() {
        // This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
        // org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [2[UNCONDITIONALDOLOOP]], but top level block is 1[CATCHBLOCK]
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:392)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:444)
        // org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:2802)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:787)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:214)
        // org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:159)
        // org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:91)
        // org.benf.cfr.reader.entities.Method.analyse(Method.java:353)
        // org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:731)
        // org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:663)
        // org.benf.cfr.reader.Main.doJar(Main.java:126)
        // org.benf.cfr.reader.Main.main(Main.java:178)
        throw new IllegalStateException("Decompilation failed");
    }

    public SwingWorker() {
        final Runnable doFinished = new Runnable(){

            public void run() {
                SwingWorker.this.finished();
            }
        };
        Runnable doConstruct = new Runnable(){

            public void run() {
                try {
                    SwingWorker.this.setValue(SwingWorker.this.construct());
                }
                finally {
                    SwingWorker.this.threadVar.clear();
                }
                SwingUtilities.invokeLater(doFinished);
            }
        };
        Thread t = new Thread(doConstruct);
        this.threadVar = new ThreadVar(t);
    }

    public void start() {
        Thread t = this.threadVar.get();
        if (t != null) {
            t.start();
        }
    }

    private static class ThreadVar {
        private Thread thread;

        ThreadVar(Thread t) {
            this.thread = t;
        }

        synchronized Thread get() {
            return this.thread;
        }

        synchronized void clear() {
            this.thread = null;
        }
    }

}

