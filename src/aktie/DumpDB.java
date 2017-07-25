package aktie;

import java.io.File;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.data.HH2Session;

public class DumpDB
{

    @SuppressWarnings ( "rawtypes" )
    private static void printTypes ( Session s, String tp )
    {
        Query q = s.createQuery ( "SELECT x FROM " + tp + " x " );
        List ll0 = q.list();

        for ( Object id : ll0 )
        {
            System.out.println ( tp );
            System.out.println ( id );
        }

    }

    public static void main ( String args[] )
    {
        if ( args.length < 1 )
        {
            System.out.println ( "Node dir must be specified." );
            System.exit ( 1 );
        }

        String nodedir = args[0];
        HH2Session session = new HH2Session();
        session.init ( nodedir + File.separator + "h2" );

        try
        {
            Session s = session.getSession();

            printTypes ( s, "IdentityData" );
            printTypes ( s, "CommunityMember" );
            printTypes ( s, "CommunityMyMember" );
            printTypes ( s, "DeveloperIdentity" );
            printTypes ( s, "DirectoryShare" );
            printTypes ( s, "Launcher" );
            printTypes ( s, "PrivateMsgIdentity" );
            printTypes ( s, "RequestFile" );
            printTypes ( s, "RequestIdentities" );

            s.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

}
