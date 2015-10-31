package aktie.net;

import java.util.Iterator;
import java.util.List;

import org.bouncycastle.crypto.params.KeyParameter;
import org.hibernate.Query;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMyMember;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.utils.DigestValidator;
import aktie.utils.SymDecoder;

public class InComProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private SymDecoder decoder;

    public InComProcessor ( HH2Session s, Index i, GuiCallback cb )
    {
        index = i;
        session = s;
        guicallback = cb;
        decoder = new SymDecoder();
        validator = new DigestValidator ( index );
    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.COMMUNITY.equals ( type ) )
        {
            if ( validator.newAndValid ( b ) )
            {
                Long seqnum = b.getNumber ( CObj.SEQNUM );
                String creatorid = b.getString ( CObj.CREATOR );
                b.pushPrivate ( CObj.MINE, "false" );

                if ( seqnum != null && creatorid != null )
                {
                    Session s = null;

                    try
                    {
                        s = session.getSession();
                        s.getTransaction().begin();
                        IdentityData id = ( IdentityData )
                                          s.get ( IdentityData.class, creatorid );

                        if ( id != null )
                        {
                            //Do not update the last number unless it is in sequence
                            //keeping track of wholes in the sequence nubmers is stupid.
                            if ( seqnum == ( id.getLastCommunityNumber() + 1 ) )
                            {
                                id.setLastCommunityNumber ( seqnum );
                                id.setNextClosestCommunityNumber ( seqnum );
                                id.setNumClosestCommunityNumber ( 1 );
                                s.merge ( id );
                            }

                            else
                            {
                                /*
                                    if there is a permanent gap in a sequence number
                                    count how many times we see the next number, so
                                    if we see it too many times we just use it for last
                                    number instead
                                */
                                if ( seqnum > id.getLastCommunityNumber() )
                                {
                                    if ( id.getNextClosestCommunityNumber() > seqnum ||
                                            id.getNextClosestCommunityNumber() <= id.getLastCommunityNumber() )
                                    {
                                        id.setNextClosestCommunityNumber ( seqnum );
                                        id.setNumClosestCommunityNumber ( 1 );
                                        s.merge ( id );
                                    }

                                    else if ( id.getNextClosestCommunityNumber() == seqnum )
                                    {
                                        id.setNumClosestCommunityNumber (
                                            id.getNumClosestCommunityNumber() + 1 );
                                        s.merge ( id );
                                    }

                                }

                            }

                        }

                        s.getTransaction().commit();

                        String scope = b.getString ( CObj.SCOPE );

                        if ( CObj.SCOPE_PRIVATE.equals ( scope ) )
                        {
                            //Get my memberships, attempt to decode - in case
                            //we got our membership before the community data
                            Query q = s.createQuery ( "SELECT x FROM CommunityMyMember x" );
                            List<CommunityMyMember> mmlst = q.list();
                            boolean decoded = false;
                            CommunityMyMember themember = null;
                            Iterator<CommunityMyMember> i = mmlst.iterator();

                            while ( i.hasNext() && !decoded )
                            {
                                CommunityMyMember cmm = i.next();
                                KeyParameter sk = new KeyParameter ( cmm.getKey() );

                                if ( decoder.decode ( b, sk ) )
                                {
                                    decoded = true;
                                    themember = cmm;
                                }

                            }

                            if ( themember != null )
                            {
                                b.pushPrivate ( CObj.KEY, Utils.toString ( themember.getKey() ) );
                                b.pushPrivate ( CObj.MINE, "true" );
                            }

                        }

                        else
                        {
                            decoder.decodeText ( b, b.getString ( CObj.PAYLOAD ), b.getString ( CObj.PAYLOAD2 ) );

                            if ( b.getPrivate ( CObj.NAME ) != null )
                            {
                                b.pushPrivate ( CObj.MINE, "true" );
                            }

                        }

                        s.close();
                        index.index ( b, true );
                        guicallback.update ( b );
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

            return true;
        }

        return false;
    }

}
