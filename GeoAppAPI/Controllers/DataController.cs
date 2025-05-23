using System.Security.Claims;
using GeoAppAPI.Dtos;
using GeoAppAPI.Dtos.Assemblers;
using GeoAppAPI.Models;
using GeoAppAPI.Services;
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
    public async Task<ActionResult<IEnumerable<DataDto>>> Get(Guid userId, 
        DateTimeOffset? from = null, 
        DateTimeOffset? to = null, 
        CancellationToken cancellationToken = default)
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
        
        logger.LogDebug("New data received");
        
        var results = await influxService
            .QueryAsync(claimUserIdString, from, to, cancellationToken)
            .ConfigureAwait(false);

        var dataDtos = DataDtoAssembler.AssembleDtos(results);
        
        return Ok(dataDtos);
    }
    
    [HttpPut]
    [Authorize(Permissions.WriteData)]
    public async Task<IActionResult> Put(Guid userId, 
        IEnumerable<DataDto> data, 
        CancellationToken cancellationToken = default)
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

        var dataModels = DataDtoAssembler.AssembleModels(data);
        
        await influxService.WriteAsync(claimUserIdString, dataModels, cancellationToken).ConfigureAwait(false);

        return Ok();
    }
}