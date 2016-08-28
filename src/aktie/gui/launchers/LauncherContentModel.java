package aktie.gui.launchers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.data.HH2Session;
import aktie.data.Launcher;

public class LauncherContentModel
{

    private HH2Session session;

    public LauncherContentModel ( HH2Session s )
    {
        session = s;
    }

    @SuppressWarnings ( "unchecked" )
    public List<Launcher> getLaunchers()
    {
        List<Launcher> l = new LinkedList<Launcher>();
        Session s = null;

        try
        {
            s = session.getSession();
            Query q = s.createQuery ( "SELECT x FROM Launcher x" );
            l = q.list();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

        finally
        {
            if ( s != null )
            {
                try
                {
                    s.close();
                }

                catch ( Exception e2 )
                {
                }

            }

        }

        return l;
    }

    public boolean open ( String localfile )
    {
        if ( localfile != null )
        {
            String bits[] = localfile.split ( "\\." );
            System.out.println ( "BITS: " + bits.length );

            if ( bits.length > 1 )
            {
                String ext = bits[bits.length - 1];
                Launcher l = null;
                Session s = null;

                try
                {
                    s = session.getSession();
                    l = ( Launcher ) s.get ( Launcher.class, ext );
                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

                finally
                {
                    if ( s != null )
                    {
                        try
                        {
                            s.close();
                        }

                        catch ( Exception e2 )
                        {
                        }

                    }

                }

                if ( l != null )
                {
                    new ProgramRunner ( l.getPath(), localfile, "." );
                    return true;
                }

            }

        }

        return false;
    }

    public void removeLauncher ( String ext )
    {
        if ( ext != null )
        {
            Session s = null;

            try
            {
                s = session.getSession();
                Launcher l = ( Launcher ) s.get ( Launcher.class, ext );

                if ( l != null )
                {
                    s.getTransaction().begin();
                    s.delete ( l );
                    s.getTransaction().commit();
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

            finally
            {
                if ( s != null )
                {
                    try
                    {
                        s.close();
                    }

                    catch ( Exception e2 )
                    {
                    }

                }

            }

        }

    }

    public void addLauncher ( String program, String extensions )
    {
        System.out.println ( "PROG: " + program + " ext: " + extensions );

        if ( extensions != null && program != null )
        {
            File f = new File ( program );

            if ( f.exists() )
            {
                System.out.println ( "F exists" );
                String extlst[] = extensions.split ( "[\\s,]+" );

                for ( int c = 0; c < extlst.length; c++ )
                {
                    String ext = extlst[c];
                    Session s = null;

                    try
                    {
                        s = session.getSession();
                        s.getTransaction().begin();
                        Launcher l = new Launcher();
                        System.out.println ( ":: " + program + " :: " + ext );
                        l.setExtension ( ext );
                        l.setPath ( program );
                        s.merge ( l );
                        s.getTransaction().commit();
                    }

                    catch ( Exception e )
                    {
                        e.printStackTrace();

                        if ( s != null )
                        {
                            try
                            {
                                if ( s.getTransaction().isActive() )
                                {
                                    s.getTransaction().rollback();
                                }

                            }

                            catch ( Exception e2 )
                            {
                            }

                        }

                    }

                    finally
                    {
                        if ( s != null )
                        {
                            try
                            {
                                s.close();
                            }

                            catch ( Exception e2 )
                            {
                            }

                        }

                    }

                }

            }

        }

    }

}
