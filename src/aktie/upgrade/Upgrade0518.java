package aktie.upgrade;

import java.io.File;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.data.HH2Session;
import aktie.data.Launcher;
import aktie.gui.subtree.SubTreeEntity;

public class Upgrade0518
{

    @SuppressWarnings ( "unchecked" )
    public static void upgrade ( String nodeDir )
    {
        HH2Session gsession = new HH2Session();
        gsession.init ( nodeDir + File.separator + "h2gui" );

        HH2Session session = new HH2Session();
        session.init ( nodeDir + File.separator + "h2" );

        Session gs = gsession.getSession();
        Session s = session.getSession();

        try
        {
            gs.getTransaction().begin();

            Query q = s.createQuery ( "SELECT x FROM SubTreeEntity x" );
            List<SubTreeEntity> r = q.list();

            for ( SubTreeEntity e : r )
            {
                gs.merge ( e );
            }

            gs.getTransaction().commit();
            gs.getTransaction().begin();

            q = s.createQuery ( "SELECT x FROM Launcher x" );
            List<Launcher> rl = q.list();

            for ( Launcher e : rl )
            {
                gs.merge ( e );
            }

            gs.getTransaction().commit();
        }

        catch ( Exception e )
        {
            e.printStackTrace();

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

            try
            {
                if ( gs.getTransaction().isActive() )
                {
                    gs.getTransaction().rollback();
                }

            }

            catch ( Exception e2 )
            {
            }

        }

        gs.close();
        s.close();

        gsession.close();
        session.close();
    }

}
