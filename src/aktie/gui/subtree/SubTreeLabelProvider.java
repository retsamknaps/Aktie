package aktie.gui.subtree;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

public class SubTreeLabelProvider implements IStyledLabelProvider
{

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

        }

        return null;
    }

    @Override
    public StyledString getStyledText ( Object c )
    {
        if ( c != null && c instanceof SubTreeEntity )
        {
            SubTreeEntity e = ( SubTreeEntity ) c;
            return new StyledString ( e.getText() );
        }

        return null;
    }

}
