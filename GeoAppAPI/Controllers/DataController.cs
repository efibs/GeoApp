using GeoAppAPI.Models;
using InfluxDB.Client;
using InfluxDB.Client.Linq;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GeoAppAPI.Controllers;

[Authorize]
[ApiController]
[Route("/api/data")]
public class DataController(ILogger<DataController> logger, InfluxDBClient influxDbClient)
    : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IEnumerable<Data>>> Get()
    {
        var query = influxDbClient.GetQueryApi();

        var results = await InfluxDBQueryable<Data>
            .Queryable(bucket: "GeoApp", "docs", query)
            .GetAsyncEnumerator()
            .ToListAsync()
            .ConfigureAwait(false);
        
        return Ok(results);
    }

    [HttpPut]
    public async Task<IActionResult> Put(Data data)
    {
        var write = influxDbClient.GetWriteApiAsync();

        await write.WriteMeasurementAsync(data, bucket: "GeoApp", org: "docs").ConfigureAwait(false);

        return Ok();
    }
}