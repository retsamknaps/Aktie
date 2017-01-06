package aktie.net;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.user.IdentityManager;

public class ReqGlobalSeq extends GenericProcessor
{

    private ConnectionThread conThread;
    private IdentityManager identManager;
    private Index index;

    public ReqGlobalSeq ( Index i, IdentityManager im, ConnectionThread ct )
    {
        conThread = ct;
        identManager = im;
        index = i;
    }

    @Override
    public boolean process ( CObj b )
    {
        if ( CObj.CON_REQ_GLOBAL.equals ( b.getType() ) )
        {
            Long last = b.getNumber ( CObj.SEQNUM );

            System.out.println ( "REQUEST GLOBAL SEQ: " + last + " ME: " + conThread.getLocalDestination().getIdentity().getId() );

            if ( last != null )
            {
                long curseq = identManager.getGlobalSequenceNumber (
                                  conThread.getLocalDestination().getIdentity().getId() );

                //Get all objects at that sequence number.
                CObjList seqobj = index.getGlobalSeqNumbers (
                                      conThread.getLocalDestination().getIdentity().getId(),
                                      last, curseq );

                System.out.println ( "RETURNING " + seqobj.size() + " DIGESTS." );
                //Check that the requesting node has access.
                long seqnum = -1;
                CObjList rlst = new CObjList();
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
                            seqdone = true;
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

                if ( rlst.size() > 0 && seqnum > -1 )
                {
                    conThread.enqueue ( rlst );
                    CObj lc = new CObj();
                    lc.setType ( CObj.SEQCOMP );
                    lc.pushNumber ( CObj.SEQNUM, seqnum );
                    conThread.enqueue ( lc );
                    System.out.println ( "SENT LAST SEQUENCE! " +
                                         conThread.getLocalDestination().getIdentity().getId() +
                                         " SEQ: " + seqnum );
                }

            }

            return true;
        }

        return false;
    }

    private void sendDig ( CObjList lst, CObj d )
    {
        String dig = d.getDig();
        CObj ds = new CObj();
        ds.setType ( CObj.OBJDIG );
        ds.setDig ( dig );
        System.out.println ( "RETURNING DIG: " + dig );
        lst.add ( ds );
    }

}
