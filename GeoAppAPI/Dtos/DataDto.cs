namespace GeoAppAPI.Models;

public class DataDto
{
    public double Latitude { get; set; }

    public double Longitude { get; set; }

    public int Steps { get; set; }

    public DateTimeOffset Timestamp { get; set; }
}