package aktie.net;

import java.util.logging.Logger;

import aktie.GenericProcessor;
import aktie.data.CObj;
import aktie.data.DeveloperIdentity;
import aktie.index.Index;
import aktie.spam.SpamTool;
import aktie.user.IdentityManager;
import aktie.utils.DigestValidator;

public class InDeveloperProcessor extends GenericProcessor
{

    Logger log = Logger.getLogger ( "aktie" );

    private Index index;
    private DigestValidator validator;
    private ConnectionThread connection;
    private CObj ConId;
    private IdentityManager identManager;

    public InDeveloperProcessor ( Index i, SpamTool st, IdentityManager im )
    {
        index = i;
        identManager = im;
        validator = new DigestValidator ( index, st );
    }

    @Override
    public boolean process ( CObj b )
    {
        String type = b.getType();

        if ( CObj.DEVELOPER.equals ( type ) )
        {
            log.info ( "NEW DEVELOPER ID: " + b.getString ( CObj.IDENTITY ) + " dig: " + b.getDig() );

            if ( validator.valid ( b ) )
            {
                boolean isnew = ( null == index.getByDig ( b.getDig() ) );

                //Update creator's ident index
                String creator = b.getString ( CObj.CREATOR );
                String devid = b.getString ( CObj.IDENTITY );

                log.info ( "NEW DEVELOPER ID valid: " + b.getString ( CObj.IDENTITY ) + " dig: " + b.getDig() +
                           " creator: " + creator + " devid: " + devid );

                if ( creator != null && devid != null )
                {

                    DeveloperIdentity di = identManager.getDeveloperIdentity ( creator );

                    if ( di != null )
                    {
                        try
                        {
                            //sseq.getObj() is only set if a prior developer identity was added
                            if ( isnew )
                            {
                                if ( identManager != null && ConId != null )
                                {
                                    long gseq = identManager.getGlobalSequenceNumber ( ConId.getId(), false );
                                    b.pushPrivateNumber ( CObj.getGlobalSeq ( ConId.getId() ), gseq );
                                }

                                log.info ( "NEW DEVELOPER ID INDEX: " + b.getString ( CObj.IDENTITY ) + " dig: " + b.getDig() );

                                index.index ( b );
                                identManager.newDeveloperIdentity ( devid );
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

    @Override
    public void setContext ( Object c )
    {
        connection = ( ConnectionThread ) c;
        ConId = connection.getLocalDestination().getIdentity();
    }


}
