package aktie.gui.subtree;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import aktie.gui.SWTApp;

public class SubTreeLabelProvider implements IStyledLabelProvider
{

    private Styler blueStyle;

    public SubTreeLabelProvider ( SubTreeModel m )
    {
        blueStyle = new Styler()
        {

            @Override
            public void applyStyles ( TextStyle a )
            {
                a.foreground = Display.getDefault().getSystemColor ( SWT.COLOR_LIST_SELECTION_TEXT );
                a.background = Display.getDefault().getSystemColor ( SWT.COLOR_LIST_SELECTION );
            }

        };

    }

    @Override
    public void addListener ( ILabelProviderListener arg0 )
    {

    }

    @Override
    public void dispose()
    {

    }

    @Override
    public boolean isLabelProperty ( Object arg0, String arg1 )
    {
        return false;
    }

    @Override
    public void removeListener ( ILabelProviderListener arg0 )
    {

    }

    @Override
    public Image getImage ( Object c )
    {
        if ( c != null && c instanceof SubTreeEntity )
        {
            SubTreeEntity s = ( SubTreeEntity ) c;

            if ( SubTreeEntity.IDENTITY_TYPE == s.getType() )
            {
                if ( s.isConnected() )
                {
                    return SWTApp.imgReg.get ( "online" );
                }

                else
                {
                    return SWTApp.imgReg.get ( "offline" );
                }

            }

            if ( SubTreeEntity.FOLDER_TYPE == s.getType() )
            {
                return SWTApp.imgReg.get ( "folder" );
            }

            if ( SubTreeEntity.PRVCOMMUNITY_TYPE == s.getType() ||
                    SubTreeEntity.PRVMESSAGE_TYPE == s.getType() )
            {
                return SWTApp.imgReg.get ( "privsub" );
            }

            if ( SubTreeEntity.PUBCOMMUNITY_TYPE == s.getType() )
            {
                return SWTApp.imgReg.get ( "pubsub" );
            }

        }

        return null;
    }

    @Override
    public StyledString getStyledText ( Object c )
    {
        if ( c != null && c instanceof SubTreeEntity )
        {
            SubTreeEntity e = ( SubTreeEntity ) c;
            String txt = e.getText();

            if ( txt == null )
            {
                txt = "";
            }

            if ( e.isBlue() )
            {
                return new StyledString ( txt, blueStyle );
            }

            return new StyledString ( txt );
        }

        return null;
    }

}
