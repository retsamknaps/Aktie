package aktie.gui;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;
import aktie.gui.IdentitySubTreeProvider.TreeIdentity;
import aktie.gui.IdentitySubTreeProvider.TreeSubscription;

public class IdentitySubTreeLabelProvider implements IStyledLabelProvider
{

    private Styler blueStyle;
    private Styler redStyle;

    public IdentitySubTreeLabelProvider()
    {
        blueStyle = new Styler()
        {

            @Override
            public void applyStyles ( TextStyle a )
            {
                a.foreground = Display.getDefault().getSystemColor ( SWT.COLOR_BLUE );
            }

        };

        redStyle = new Styler()
        {

            @Override
            public void applyStyles ( TextStyle a )
            {
                a.foreground = Display.getDefault().getSystemColor ( SWT.COLOR_RED );
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
    public Image getImage ( Object arg0 )
    {
        return null;
    }

    @Override
    public StyledString getStyledText ( Object a )
    {
        if ( a instanceof TreeIdentity )
        {
            TreeIdentity e = ( TreeIdentity ) a;
            Long np = e.identity.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );
            Long open = e.identity.getPrivateNumber ( CObj.PRV_DEST_OPEN );

            if ( open != null && open == 0L )
            {
                return new StyledString ( e.identity.getDisplayName() + " (OFF)", redStyle );
            }

            if ( np != null && np == 1L )
            {
                return new StyledString ( e.identity.getDisplayName(), blueStyle );
            }

            return new StyledString ( e.identity.getDisplayName() );
        }

        else if ( a instanceof TreeSubscription )
        {
            TreeSubscription ts = ( TreeSubscription ) a;
            String scope = ts.community.getString ( CObj.SCOPE );
            String name = ts.community.getPrivateDisplayName();

            if ( CObj.SCOPE_PRIVATE.equals ( scope ) )
            {
                name = "* " + name;
            }

            Long np = ts.community.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

            if ( np != null && np == 1L )
            {
                return new StyledString ( name, blueStyle );
            }

            return new StyledString ( name );
        }

        return null;
    }

}
