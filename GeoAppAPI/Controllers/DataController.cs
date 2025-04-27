using System.Security.Claims;
using GeoAppAPI.Models;
using GeoAppAPI.Services;
using InfluxDB.Client;
using InfluxDB.Client.Linq;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace GeoAppAPI.Controllers;

[ApiController]
[Route("/api/data/{userId}")]
public class DataController(ILogger<DataController> logger, InfluxService influxService)
    : ControllerBase
{
    [HttpGet]
    [Authorize(Permissions.ReadData)]
    public async Task<ActionResult<IEnumerable<Data>>> Get(Guid userId)
    {
        var claimUserIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(claimUserIdString))
        {
            return Unauthorized();
        }

        var claimUserId = Guid.Parse(claimUserIdString);
        if (claimUserId != userId)
        {
            return Unauthorized();
        }
        
        var results = await influxService.QueryAsync(claimUserIdString).ConfigureAwait(false);
        
        return Ok(results);
    }
    
    [HttpPut]
    [Authorize(Permissions.WriteData)]
    public async Task<IActionResult> Put(Guid userId, IEnumerable<Data> data)
    {
        var claimUserIdString = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(claimUserIdString))
        {
            return Unauthorized();
        }

        var claimUserId = Guid.Parse(claimUserIdString);
        if (claimUserId != userId)
        {
            return Unauthorized();
        }
        
        await influxService.WriteAsync(claimUserIdString, data).ConfigureAwait(false);

        return Ok();
    }
}