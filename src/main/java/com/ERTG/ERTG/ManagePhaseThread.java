package com.ERTG.ERTG;

import java.util.concurrent.atomic.AtomicLong;

public class ManagePhaseThread implements Runnable{

    public static AtomicLong lastGenerationStartTime = new AtomicLong(0); //timestamps dell'ultimo avvio di generazione di una routing table


    @Override
    public void run() {


        System.out.println("FASE BONDING");
        Main.terminalStandardOutput.println("FASE BONDING");

        try
        {
            Thread.currentThread().sleep(DefaultConfigurationValues.DEFAULT_BONDING_PREFERENCE_TIME[0]);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }


        Main.startBondingPreferancePhase.set(false);
        Main.startFindNodePreferencePhase.set(true);

        System.out.println("FASE FINDNODE");
        Main.terminalStandardOutput.println("FASE FINDNODE");


        try
        {
            Thread.currentThread().sleep(DefaultConfigurationValues.DEFAULT_FINDNODE_PREFERENCE_TIME[0]);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        Main.startFindNodePreferencePhase.set(false);

        System.out.println("FASE BILANCIATA");
        Main.terminalStandardOutput.println("FASE BILANCIATA");

        try
        {
            Thread.currentThread().sleep(DefaultConfigurationValues.DEFAULT_BALANCED_PREFERENCE_TIME[0]);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }




        System.out.println("FASE GENERAZIONE ROUTING TABLE");
        Main.terminalStandardOutput.println("FASE GENERAZIONE ROUTING TABLE");

        Main.retrievePeerToGenerateRoutingTable();


        System.out.println("AVVIATA GENERAZIONE PRIMA ROUTING TABLE");
        Main.terminalStandardOutput.println("AVVIATA GENERAZIONE PRIMA ROUTING TABLE");


        try{

            synchronized (Main.nodeFinder.enodeToGenerate){

                while(!Main.nodeFinder.enodeToGenerate.isEmpty()) {
                    Main.nodeFinder.enodeToGenerate.wait();
                }

            }

        }catch(InterruptedException e){
            e.printStackTrace();
        }

        Main.terminalStandardOutput.println("RIPROVO GENERAZIONE CON UN NUOVO ID");

        Main.retryGenerationEmptyTableWithNewID();


        long interval = System.currentTimeMillis()-lastGenerationStartTime.get();
        while(interval<15*1000) {//FACCIO IN MODO DI ASPETTARE ALMENO 15 SECONDI DALL'ULTIMA RICHIESTA DI AVVIO DI GENERAZIONE DI UNA ROUTING TABLE
            try {
                Thread.sleep((15*1000)-interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            interval = System.currentTimeMillis()-lastGenerationStartTime.get();
        }


        System.out.println("TERMINO MANAGE_PHASE");
        Main.terminalStandardOutput.println("TERMINO MANAGE_PHASE");

        synchronized (Main.terminate){

            Main.terminate.set(true);
            Main.terminate.notifyAll();

        }

        Main.udpListener.pongsToWrite.clear();
        Main.udpListener.pingsToWrite.clear();
        Main.udpListener.findNodeToWrite.clear();


    }

}
