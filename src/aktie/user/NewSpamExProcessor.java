package aktie.user;

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.hibernate.Session;

import aktie.GenericProcessor;
import aktie.UpdateCallback;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.CommunityMember;
import aktie.data.DeveloperIdentity;
import aktie.data.HH2Session;
import aktie.data.IdentityData;
import aktie.data.PrivateMsgIdentity;
import aktie.index.CObjList;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.utils.HasFileCreator;

public class NewSpamExProcessor extends GenericProcessor
{

    private HH2Session session;
    private Index index;
    private IdentityManager identManager;
    private UpdateCallback callback;

    public NewSpamExProcessor ( HH2Session s, Index i, IdentityManager m, UpdateCallback cb )
    {
        session = s;
        identManager = m;
        index = i;
        callback = cb;
    }

    private void saveExSpam ( CObj c, String creator, RSAPrivateCrtKeyParameters pkey )
    {
        //Set the sequence number
        Session s = null;
        long lastpostnum = 0;

        try
        {
            s = session.getSession();
            s.getTransaction().begin();
            DeveloperIdentity m = ( DeveloperIdentity ) s.get ( DeveloperIdentity.class, creator );

            if ( m == null )
            {
                throw new Exception ( "DeveloperIdentity not found! " + creator );
            }

            lastpostnum = m.getLastSpamExNumber();
            lastpostnum++;
            c.pushNumber ( CObj.SEQNUM, lastpostnum );
            m.setLastSpamExNumber ( lastpostnum );
            s.merge ( m );
            s.getTransaction().commit();
            s.close();

            c.signX ( pkey, 0 );
            c.pushPrivate ( CObj.PRV_PUSH_REQ, "true" );
            c.pushPrivateNumber ( CObj.PRV_PUSH_TIME, System.currentTimeMillis() );

            long gseq = identManager.getGlobalSequenceNumber ( creator, false );
            c.pushPrivateNumber ( CObj.getGlobalSeq ( creator ), gseq );

            index.index ( c );
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

    @Override
    public boolean process ( CObj o )
    {
        String type = o.getType();

        if ( CObj.SPAMEXCEPTION.equals ( type ) )
        {
            String creator = o.getString ( CObj.CREATOR );

            if ( creator == null )
            {
                o.pushString ( CObj.ERROR, "Creator must be specified." );
                callback.update ( o );
                return true;
            }

            DeveloperIdentity di = identManager.getDeveloperIdentity ( creator );

            if ( di == null )
            {
                o.pushString ( CObj.ERROR, "Developer Identity not specified." );
                callback.update ( o );
                return true;
            }

            CObj devid = index.getMyIdentity ( creator );

            if ( devid == null )
            {
                o.pushString ( CObj.ERROR, "Not your identity." );
                callback.update ( o );
                return true;
            }

            RSAPrivateCrtKeyParameters pkey = Utils.privateKeyFromString ( devid.getPrivate ( CObj.PRIVATEKEY ) );

            if ( pkey == null )
            {
                o.pushString ( CObj.ERROR, "Private key not found!" );
                callback.update ( o );
                return true;
            }

            String save = o.getPrivate ( CObj.STATUS );

            //Get all IdentityData.
            CObjList idlst = index.getIdentities();

            for ( int c = 0; c < idlst.size(); c++ )
            {
                try
                {
                    CObj ident = idlst.get ( c );
                    String sid = SpamTool.EXSPAMPREFIX + ident.getId();
                    CObj nex = new CObj();
                    nex.setType ( CObj.SPAMEXCEPTION );
                    nex.setId ( sid );
                    nex.pushString ( CObj.CREATOR, creator );
                    IdentityData dat = identManager.getIdentity ( ident.getId() );

                    if ( dat != null )
                    {
                        nex.pushNumber ( CObj.COMMUNITY, dat.getLastCommunityNumber() );
                        nex.pushNumber ( CObj.MEMBERSHIP, dat.getLastMembershipNumber() );
                        nex.pushNumber ( CObj.SUBSCRIPTION, dat.getLastSubNumber() );
                    }

                    PrivateMsgIdentity pdat = identManager.getPrvMsgIdentityData ( ident.getId() );

                    if ( pdat != null )
                    {
                        nex.pushNumber ( CObj.PRIVIDENTIFIER, pdat.getLastIdentNumber() );
                        nex.pushNumber ( CObj.PRIVMESSAGE, pdat.getLastMsgNumber() );
                    }

                    if ( "save".equals ( save ) )
                    {
                        saveExSpam ( nex, creator, pkey );
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            idlst.close();


            CObjList sbs = index.getMySubscriptions();

            for ( int d = 0; d < sbs.size(); d++ )
            {
                try
                {
                    CObj msub = sbs.get ( d );
                    String comid = msub.getString ( CObj.COMMUNITYID );
                    String comname = "ERROR";

                    if ( !"save".equals ( save ) )
                    {
                        CObj comm = index.getCommunity ( comid );

                        if ( comm != null )
                        {
                            comname = comm.getPrivateDisplayName();
                        }

                    }

                    if ( comid != null )
                    {
                        CObjList osbs = index.getSubscriptions ( comid, null );

                        for ( int e = 0; e < osbs.size(); e++ )
                        {
                            try
                            {
                                CObj sub = osbs.get ( e );
                                String creatorid = sub.getString ( CObj.CREATOR );
                                String cid = HasFileCreator.getCommunityMemberId ( creatorid, comid );

                                if ( !"save".equals ( save ) )
                                {
                                    String subname = "ERROR";
                                    CObj sbr = index.getIdentity ( creatorid );

                                    if ( sbr != null )
                                    {
                                        subname = sbr.getDisplayName();
                                    }

                                    StringBuilder sb = new StringBuilder();
                                    sb.append ( comname );
                                    sb.append ( " ::: " );
                                    sb.append ( subname );
                                }

                                CommunityMember cm = identManager.getCommunityMember ( cid );

                                if ( cm != null )
                                {
                                    CObj ccm = new CObj();
                                    ccm.setType ( CObj.SPAMEXCEPTION );
                                    ccm.setId ( SpamTool.EXSPAMPREFIX + cid );
                                    ccm.pushString ( CObj.CREATOR, creator );
                                    ccm.pushNumber ( CObj.HASFILE, cm.getLastFileNumber() );
                                    ccm.pushNumber ( CObj.POST, cm.getLastPostNumber() );

                                    if ( "save".equals ( save ) )
                                    {
                                        saveExSpam ( ccm, creator, pkey );
                                    }

                                }

                            }

                            catch ( Exception e2 )
                            {
                                e2.printStackTrace();
                            }

                        }

                        osbs.close();
                    }

                }

                catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }

            sbs.close();

            return true;
        }

        return false;
    }

}
