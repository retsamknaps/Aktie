package aktie.utils;

public class NeverHappen
{

    public static void never()
    {
        System.out.println ( "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
        System.out.println ( "ERROR: We should never get here!  Send to devlopers please." );
        Thread.dumpStack();
        System.out.println ( "Thank you!" );
        System.out.println ( "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" );
    }

}
