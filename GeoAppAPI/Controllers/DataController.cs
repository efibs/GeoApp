using GeoAppAPI.Models;
using InfluxDB.Client;
using InfluxDB.Client.Linq;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GeoAppAPI.Controllers;

[ApiController]
[Route("/api/data/{userId}")]
public class DataController(ILogger<DataController> logger, InfluxDBClient influxDbClient)
    : ControllerBase
{
    [HttpGet]
    [Authorize(Permissions.ReadData)]
    public async Task<ActionResult<IEnumerable<Data>>> Get(Guid userId)
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
    [Authorize(Permissions.WriteData)]
    public async Task<IActionResult> Put(Guid userId, Data data)
    {
        var write = influxDbClient.GetWriteApiAsync();

        await write.WriteMeasurementAsync(data, bucket: "GeoApp", org: "docs").ConfigureAwait(false);

        return Ok();
    }
}