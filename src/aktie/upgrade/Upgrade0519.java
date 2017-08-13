package aktie.upgrade;

import java.io.File;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import aktie.data.HH2Session;
import aktie.gui.subtree.SubTreeEntity;

public class Upgrade0519
{

    @SuppressWarnings ( "unchecked" )
    private static void saveEntry ( Session source, Session target, SubTreeEntity e )
    {
        long sourceid = e.getId();

        target.getTransaction().begin();
        SubTreeEntity ne = ( SubTreeEntity ) target.merge ( e );
        target.getTransaction().commit();

        Query q = source.createQuery ( "SELECT x FROM SubTreeEntity x WHERE x.parent = :prt" );
        q.setParameter ( "prt", sourceid );
        List<SubTreeEntity> r = q.list();

        for ( SubTreeEntity et : r )
        {
            et.setParent ( ne.getId() );
            saveEntry ( source, target, et );
        }

    }

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

            Query q = gs.createQuery ( "DELETE SubTreeEntity" );
            q.executeUpdate();

            gs.getTransaction().commit();

            q = s.createQuery ( "SELECT x FROM SubTreeEntity x WHERE x.parent = :prt" );
            q.setParameter ( "prt", 0L );
            List<SubTreeEntity> r = q.list();

            for ( SubTreeEntity e : r )
            {
                saveEntry ( s, gs, e );
            }

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
