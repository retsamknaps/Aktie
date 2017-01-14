package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class ReqGlobalSeq extends GenericProcessor
{
    Logger log = Logger.getLogger ( "aktie" );

    private ConnectionThread conThread;
    private IdentityManager identManager;
    private Index index;

    public ReqGlobalSeq ( Index i, IdentityManager im, ConnectionThread ct )
    {
        conThread = ct;
        identManager = im;
        index = i;
    }

    private long filterObjects ( CObjList seqobj, CObjList rlst )
    {
        //Check that the requesting node has access.
        long seqnum = -1;
        boolean seqdone = false;

        for ( int c = 0; c < seqobj.size() && !seqdone; c++ )
        {
            try
            {
                CObj o = seqobj.get ( c );

                long gseq = o.getPrivateNumber (
                                CObj.getGlobalSeq ( conThread.getLocalDestination().getIdentity().getId() ) );

                if ( seqnum == -1 )
                {
                    seqnum = gseq;
                }

                if ( seqnum != gseq )
                {
                    if ( rlst.size() == 0 )
                    {
                        seqnum = gseq;
                    }

                    else
                    {
                        seqdone = true;
                    }

                }

                if ( !seqdone )
                {
                    //MUST BE SUBSCRIBERS FOR: POST/HASFILE
                    if ( CObj.POST.equals ( o.getType() )  ||
                            CObj.HASFILE.equals ( o.getType() ) )
                    {
                        String comid = o.getString ( CObj.COMMUNITYID );

                        if ( comid != null )
                        {
                            if ( conThread.getSubs().contains ( comid ) )
                            {
                                sendDig ( rlst, o );
                            }

                        }

                    }

                    if ( CObj.SUBSCRIPTION.equals ( o.getType() ) )
                    {
                        String comid = o.getString ( CObj.COMMUNITYID );

                        if ( conThread.getMemberships().contains ( comid ) )
                        {
                            //Just send it if they're already members
                            sendDig ( rlst, o );
                        }

                        else
                        {
                            CObj com = index.getCommunity ( comid );

                            if ( com != null )
                            {
                                String pp = com.getString ( CObj.SCOPE );

                                if ( CObj.SCOPE_PUBLIC.equals ( pp ) )
                                {
                                    sendDig ( rlst, o );
                                }

                            }

                        }

                    }

                    if ( CObj.MEMBERSHIP.equals ( o.getType() ) ||
                            CObj.PRIVIDENTIFIER.equals ( o.getType() ) ||
                            CObj.PRIVMESSAGE.equals ( o.getType() ) ||
                            CObj.COMMUNITY.equals ( o.getType() ) ||
                            CObj.IDENTITY.equals ( o.getType() ) ||
                            CObj.SPAMEXCEPTION.equals ( o.getType() ) )
                    {
                        //ANYONE: MEMBERSHIP, PRIVIDENT,
                        //        PRIVMESSAGE, COMMUNITY, IDENTITY, SPAMEXCEPTION
                        sendDig ( rlst, o );
                    }

                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        seqobj.close();

        return seqnum;
    }

    private void log ( String msg )
    {
        if ( conThread.getEndDestination() != null )
        {
            log.info ( "ME: " + conThread.getLocalDestination().getIdentity().getId() +
                       " FROM: " + conThread.getEndDestination().getId() + " " + msg );
        }

    }

    private void sendDig ( CObjList lst, CObj d )
    {
        String dig = d.getDig();
        CObj ds = new CObj();
        ds.setType ( CObj.OBJDIG );
        ds.setDig ( dig );
        lst.add ( ds );
        log ( "SEND: " + dig );
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CON_REQ_GLOBAL.equals ( b.getType() ) )
        {
            Long publast = b.getNumber ( CObj.SEQNUM );
            Long memlast = b.getNumber ( CObj.MEMSEQNUM );
            Long sublast = b.getNumber ( CObj.SUBSEQNUM );

            long curseq = identManager.getMyLastGlobalSequenceNumber (
                              conThread.getLocalDestination().getIdentity().getId() );

            if ( publast != null && memlast != null && sublast != null )
            {

                log ( " REQ GBL SEQ: " + publast + " " + memlast + " " + sublast );
                CObjList rlst = new CObjList();

                //Get all objects at that sequence number.
                CObjList seqobj = index.getGlobalPubSeqNumbers (
                                      conThread.getLocalDestination().getIdentity().getId(),
                                      publast, curseq );
                log ( " REQ PUBS: " + publast + " sz: " + seqobj.size() );

                long ppub = filterObjects ( seqobj, rlst );
                log ( " REQ PUBS: filtered send lst: " + rlst.size() + " last seq: " + ppub );

                if ( ppub == -1 )
                {
                    ppub = publast;
                }

                //Get all objects at that sequence number.
                seqobj = index.getGlobalMemSeqNumbers (
                             conThread.getLocalDestination().getIdentity().getId(),
                             memlast, curseq );
                log ( " REQ MEM: " + memlast + " sz: " + seqobj.size() );

                long mpub = filterObjects ( seqobj, rlst );
                log ( " REQ PUBS: filtered send lst: " + rlst.size() + " last seq: " + mpub );

                if ( mpub == -1 )
                {
                    mpub = memlast;
                }

                //Get all objects at that sequence number.
                seqobj = index.getGlobalSubSeqNumbers (
                             conThread.getLocalDestination().getIdentity().getId(),
                             sublast, curseq );
                log ( " REQ SUB: " + sublast + " sz: " + seqobj.size() );

                long spub = filterObjects ( seqobj, rlst );
                log ( " REQ PUBS: filtered send lst: " + rlst.size() + " last seq: " + spub );

                if ( spub == -1 )
                {
                    spub = sublast;
                }

                if ( rlst.size() > 0 )
                {
                    conThread.enqueue ( rlst );
                    CObj lc = new CObj();
                    lc.setType ( CObj.SEQCOMP );
                    lc.pushNumber ( CObj.SEQNUM, ppub );
                    lc.pushNumber ( CObj.MEMSEQNUM, mpub );
                    lc.pushNumber ( CObj.SUBSEQNUM, spub );
                    conThread.enqueue ( lc );
                }

            }

            return true;
        }

        return false;
    }

}
