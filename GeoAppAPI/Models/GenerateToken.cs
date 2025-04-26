namespace GeoAppAPI.Models;

public class GenerateToken
{
    public TimeSpan Expiry { get; set; }

    public string[] Permissions { get; set; }
}