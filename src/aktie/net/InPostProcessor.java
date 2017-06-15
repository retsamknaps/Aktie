package aktie.net;

import java.util.List;

import aktie.GenericProcessor;
import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.data.HH2Session;
import aktie.gui.GuiCallback;
import aktie.index.Index;
import aktie.sequences.PostSequence;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;
import aktie.utils.SubscriptionValidator;

public class InPostProcessor extends GenericProcessor
{

    private GuiCallback guicallback;
    private Index index;
    private HH2Session session;
    private DigestValidator validator;
    private SubscriptionValidator subvalidator;
    private CObj destIdent;
    private CObj ConId;
    private IdentityManager identManager;

    public InPostProcessor ( CObj id, HH2Session s, Index i, SpamTool st, IdentityManager im, CObj mid, GuiCallback cb )
    {
        destIdent = id;
        index = i;
        session = s;
        guicallback = cb;
        ConId = mid;
        identManager = im;
        validator = new DigestValidator ( index, st );
        subvalidator = new SubscriptionValidator ( index );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.POST.equals ( type ) )
        {
            if ( validator.valid ( b ) )
            {
                //Make sure this identity
                String comid = b.getString ( CObj.COMMUNITYID );
                String creatorid = b.getString ( CObj.CREATOR );
                Long seqnum = b.getNumber ( CObj.SEQNUM );

                if ( !subvalidator.canPost ( comid, creatorid ) )
                {
                    //Cannot post to blog not owner of
                    return true;
                }

                CObj mysubid = subvalidator.isMyUserSubscribed ( comid, destIdent.getId() );

                if ( comid != null && creatorid != null && mysubid != null && seqnum != null )
                {
                    boolean isnew = ( null == index.getByDig ( b.getDig() ) );

                    if ( subvalidator.canSubscribe ( comid, creatorid ) )
                    {

                        try
                        {
                            String cid = Utils.mergeIds ( comid, creatorid );
                            PostSequence pseq = new PostSequence ( session );
                            pseq.setId ( cid );
                            pseq.updateSequence ( b );

                            if ( isnew )
                            {
                                //Set the rank of the post based on the rank of the
                                //user
                                CObj idty = index.getIdentity ( creatorid );

                                if ( idty != null )
                                {
                                    Long rnk = idty.getPrivateNumber ( CObj.PRV_USER_RANK );

                                    if ( rnk != null )
                                    {
                                        b.pushPrivateNumber ( CObj.PRV_USER_RANK, rnk );
                                    }

                                }

                                long seq = identManager.getGlobalSequenceNumber ( ConId.getId(), false );
                                b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), seq );

                                b.pushPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS, 1L );
                                index.index ( b );

                                //Save any new fields listed by the post
                                List<CObj> fldlist = b.listNewFields();

                                for ( CObj fld : fldlist )
                                {
                                    CObj ft = index.getByDig ( fld.getDig() );

                                    if ( ft != null )
                                    {
                                        String deflt = ft.getPrivate ( CObj.PRV_DEF_FIELD );

                                        if ( deflt != null )
                                        {
                                            fld.pushPrivate ( CObj.PRV_DEF_FIELD, deflt );
                                        }

                                    }

                                    index.index ( fld );
                                }

                                guicallback.update ( b );
                            }

                        }

                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }

                    }

                }

            }

            return true;
        }

        return false;
    }

}
