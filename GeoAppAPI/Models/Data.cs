using InfluxDB.Client.Core;

namespace GeoAppAPI.Models;

[Measurement("data")]
public class Data
{
    [Column("Latitude")] 
    public double Latitude { get; set; }
    
    [Column("Longitude")] 
    public double Longitude { get; set; }

    [Column("Steps")]
    public int Steps { get; set; }
    
    [Column(IsTimestamp = true)] 
    public DateTime Timestamp { get; set; }
}