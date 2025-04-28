namespace GeoAppAPI.Dtos;

public class GenerateTokenDto
{
    public TimeSpan Expiry { get; set; }

    public string[] Permissions { get; set; } = [];
}