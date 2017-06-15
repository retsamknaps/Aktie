package aktie.net;

import java.util.logging.Level;
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

    private void filterObjects ( CObjList seqobj, String scomid, String typ, long rn, long last )
    {
        long seqnum = -1;
        CObjList rlst = new CObjList();

        for ( int c = 0; c < seqobj.size(); c++ )
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
                    if ( rlst.size() > 0 )
                    {
                        conThread.enqueue ( rlst );
                        rlst = new CObjList();
                    }

                    CObj lc = new CObj();
                    lc.setType ( CObj.SEQCOMP );
                    lc.pushNumber ( typ, seqnum );

                    if ( scomid != null )
                    {
                        lc.pushString ( CObj.COMMUNITYID, scomid );
                    }

                    conThread.enqueue ( lc );

                    seqnum = gseq;
                }

                //MUST BE SUBSCRIBERS FOR: POST/HASFILE
                if ( CObj.POST.equals ( o.getType() )  ||
                        CObj.HASFILE.equals ( o.getType() ) )
                {
                    String comid = o.getString ( CObj.COMMUNITYID );

                    if ( comid != null )
                    {

                        if ( Level.INFO.equals ( log.getLevel() ) )
                        {
                            log ( "SEND COM DAT: comid: " + comid + " contains: " +
                                  conThread.getSubs().contains ( comid ) + " " +
                                  conThread.getSubs() );
                        }

                        if ( conThread.getSubs().contains ( comid ) )
                        {
                            sendDig ( rlst, o, rn, seqnum );
                        }

                    }

                }

                if ( CObj.SUBSCRIPTION.equals ( o.getType() ) )
                {
                    String comid = o.getString ( CObj.COMMUNITYID );

                    if ( conThread.getMemberships().contains ( comid ) )
                    {
                        //Just send it if they're already members
                        sendDig ( rlst, o, rn, seqnum );
                    }

                    else
                    {
                        CObj com = index.getCommunity ( comid );

                        if ( com != null )
                        {
                            String pp = com.getString ( CObj.SCOPE );

                            if ( CObj.SCOPE_PUBLIC.equals ( pp ) )
                            {
                                sendDig ( rlst, o, rn, seqnum );
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
                    sendDig ( rlst, o, rn, seqnum );
                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        if ( seqnum != -1 && seqnum <= last )
        {
            if ( rlst.size() > 0 )
            {
                conThread.enqueue ( rlst );
                rlst = new CObjList();
            }

            CObj lc = new CObj();
            lc.setType ( CObj.SEQCOMP );
            lc.pushNumber ( typ, seqnum );

            if ( scomid != null )
            {
                lc.pushString ( CObj.COMMUNITYID, scomid );
            }

            conThread.enqueue ( lc );
        }

        seqobj.close();
    }

    private void log ( String msg )
    {
        if ( conThread.getEndDestination() != null )
        {
            log.info ( "ME: " + conThread.getLocalDestination().getIdentity().getId() +
                       " FROM: " + conThread.getEndDestination().getId() + " " + msg );
        }

    }

    private void sendDig ( CObjList lst, CObj d, long rn, long sn )
    {
        String dig = d.getDig();
        CObj ds = new CObj();
        ds.setType ( CObj.OBJDIG );
        ds.setDig ( dig );
        lst.add ( ds );

        if ( Level.INFO.equals ( log.getLevel() ) )
        {
            log ( "SEND: rq: " + rn + " sn: " + sn + " : " + dig  + " comid: " +
                  d.getString ( CObj.COMMUNITYID ) );
        }

    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CON_REQ_GLOBAL.equals ( b.getType() ) )
        {
            Long publast = b.getNumber ( CObj.SEQNUM );
            Long memlast = b.getNumber ( CObj.MEMSEQNUM );
            //Long sublast = b.getNumber ( CObj.SUBSEQNUM );

            String comid = b.getString ( CObj.COMMUNITYID );

            long curseq = identManager.getMyLastGlobalSequenceNumber (
                              conThread.getLocalDestination().getIdentity().getId() );

            if ( Level.INFO.equals ( log.getLevel() ) )
            {
                log ( " REQ GBL SEQ: " + publast + " " + memlast + " comid: " + comid ); //+ " " + sublast );
            }

            if ( comid == null )
            {
                if ( publast != null && memlast != null )  // && sublast != null )
                {

                    //Get all objects at that sequence number.
                    CObjList seqobj = index.getGlobalPubSeqNumbers (
                                          conThread.getLocalDestination().getIdentity().getId(),
                                          publast, curseq );

                    if ( Level.INFO.equals ( log.getLevel() ) )
                    {
                        log ( " REQ PUBS: " + publast + " sz: " + seqobj.size() );
                    }

                    filterObjects ( seqobj, null, CObj.SEQNUM, publast, curseq );

                    //Get all objects at that sequence number.
                    seqobj = index.getGlobalMemSeqNumbers (
                                 conThread.getLocalDestination().getIdentity().getId(),
                                 memlast, curseq );

                    if ( Level.INFO.equals ( log.getLevel() ) )
                    {
                        log ( " REQ MEM: " + memlast + " sz: " + seqobj.size() );
                    }

                    filterObjects ( seqobj, null, CObj.MEMSEQNUM, memlast, curseq );

                    /*
                        //Get all objects at that sequence number.
                        seqobj = index.getGlobalSubSeqNumbers (
                            conThread.getLocalDestination().getIdentity().getId(),
                            sublast, curseq );
                        if (Level.INFO.equals(log.getLevel())) {
                        log ( " REQ SUB: " + sublast + " sz: " + seqobj.size() );
                        }

                        filterObjects ( seqobj, null, CObj.SUBSEQNUM, sublast );
                    */
                }

            }

            else
            {
                if ( publast != null )
                {
                    CObjList seqobj = index.getCommunitySeqNumbers (
                                          conThread.getLocalDestination().getIdentity().getId(),
                                          comid, publast, curseq );

                    if ( Level.INFO.equals ( log.getLevel() ) )
                    {
                        log ( " REQ COMDATA: " + publast + " sz: " + seqobj.size() + " comid: " + comid );
                    }

                    filterObjects ( seqobj, comid, CObj.SEQNUM, publast, curseq );
                }

            }

            return true;
        }

        return false;
    }

}
