package aktie.net;

import java.util.List;

import org.bouncycastle.crypto.digests.RIPEMD256Digest;
import org.hibernate.Query;
import org.hibernate.Session;

import aktie.CObjProcessor;
import aktie.ContextObject;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.data.RequestFile;
import aktie.index.Index;

public class InFragProcessor implements CObjProcessor
{

    private UpdateCallback guicallback;
    private Index index;
    private HH2Session session;

    public InFragProcessor ( HH2Session s, Index i )
    {
        index = i;
        session = s;
    }

    @Override
    public boolean process ( CObj b )
    {
        return false;
    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public boolean processObj ( Object coo )
    {
        if ( coo instanceof ContextObject )
        {
            ContextObject cto = ( ContextObject ) coo;
            setContext ( cto.context );

            if ( cto.obj instanceof List )
            {
                boolean correctlist = true;
                String expdigofdig = null;
                String wholedig = null;
                RIPEMD256Digest digofdig = new RIPEMD256Digest();
                List<CObj> l = ( List<CObj> ) cto.obj;
                long offset = 0;

                for ( CObj co : l )
                {
                    if ( !CObj.FRAGMENT.equals ( co.getType() ) )
                    {
                        correctlist = false;
                    }

                    else
                    {
                        long sz = co.getNumber ( CObj.FRAGSIZE );
                        long foff = co.getNumber ( CObj.FRAGOFFSET );
                        expdigofdig = co.getString ( CObj.FRAGDIGEST );
                        wholedig = co.getString ( CObj.FILEDIGEST );

                        if ( foff == offset )
                        {
                            offset += sz;
                            byte fdig[] = Utils.toByteArray ( co.getString ( CObj.FRAGDIG ) );
                            digofdig.update ( fdig, 0, fdig.length );
                        }

                    }

                }

                if ( expdigofdig != null && wholedig != null && correctlist )
                {
                    byte ndig[] = new byte[digofdig.getDigestSize()];
                    digofdig.doFinal ( ndig, 0 );
                    String ndstr = Utils.toString ( ndig );

                    if ( ndstr.equals ( expdigofdig ) )
                    {
                        Session s = null;

                        try
                        {
                            for ( CObj co : l )
                            {
                                co.pushPrivate ( CObj.COMPLETE, "false" );
                                index.index ( co, true );
                            }

                            index.forceNewSearcher();

                            s = session.getSession();
                            s.getTransaction().begin();
                            Query q = s.createQuery ( "SELECT x FROM RequestFile x WHERE x.wholeDigest = :wdig AND "
                                                      + "x.fragmentDigest = :fdig AND x.state = :st" );
                            q.setParameter ( "fdig", ndstr );
                            q.setParameter ( "wdig", wholedig );
                            q.setParameter ( "st", RequestFile.REQUEST_FRAG_LIST_SNT );
                            List<RequestFile> rfl = q.list();

                            for ( RequestFile rf : rfl )
                            {
                                //Ok, we have the list, now go get the fragments.
                                rf.setState ( RequestFile.REQUEST_FRAG );
                                s.merge ( rf );
                                guicallback.update ( rf );
                            }

                            s.getTransaction().commit();
                            s.close();
                        }

                        catch ( Exception e )
                        {
                            if ( s != null )
                            {
                                try
                                {
                                    s.getTransaction().rollback();
                                }

                                catch ( Exception e2 )
                                {
                                }

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

                cto.notifyProcessed();
                return correctlist;

            }

        }

        return false;
    }

    @Override
    public void setContext ( Object c )
    {
        ConnectionThread ct = ( ConnectionThread ) c;
        guicallback = ct;
    }

}
