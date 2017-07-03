package aktie.upgrade;

public interface UpgradeControllerCallback
{

    public boolean doUpgrade();

    public void updateMessage ( String msg );

}
