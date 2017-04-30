package aktie.gui.launchers;

import java.util.List;

import aktie.data.Launcher;
import aktie.gui.table.AktieTableContentProvider;

public class LauncherContentProvider extends AktieTableContentProvider<List<Launcher>, Launcher>
{
    @Override
    public Launcher[] getElements ( Object a )
    {
        if ( a != null && a instanceof List )
        {
            @SuppressWarnings ( "unchecked" )
            List<Launcher> list = ( List<Launcher> ) a;
            Launcher launchers[] = new Launcher[list.size()];
            int i = 0;

            for ( Launcher launcher : list )
            {
                launchers[i] = launcher;
                i++;
            }

            return launchers;
        }

        return new Launcher[] {};

    }

}
