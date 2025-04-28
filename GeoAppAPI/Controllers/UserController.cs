using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using GeoAppAPI.Dtos;
using GeoAppAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;

namespace GeoAppAPI.Controllers;

[ApiController]
[Route("/api/users")]
public class UserController(IConfiguration config, UserManager<User> userManager)
    : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<JwtTokenDto>> RegisterUser([FromBody] RegisterDto registerDto)
    {
        var user = new User
        {
            UserName = registerDto.Username,
        };
        var result = await userManager.CreateAsync(user, registerDto.Password).ConfigureAwait(false);
        if (!result.Succeeded)
        {
            return BadRequest(result.Errors);
        }

        var createdUser = await userManager.FindByNameAsync(registerDto.Username).ConfigureAwait(false);

        if (createdUser == null)
        {
            return StatusCode(500);
        }

        var token = _generateJSONWebToken(createdUser, TimeSpan.FromHours(2), Permissions.All);

        return Ok(new JwtTokenDto { Token = token });
    }

    [HttpPost]
    [Route("login")]
    public async Task<ActionResult<JwtTokenDto>> Login([FromBody] LoginDto loginDto)
    {
        var user = await userManager.FindByNameAsync(loginDto.Username).ConfigureAwait(false);

        if (user == null)
        {
            return Unauthorized();
        }

        var passwordValid = await userManager.CheckPasswordAsync(user, loginDto.Password).ConfigureAwait(false);

        if (!passwordValid)
        {
            return Unauthorized();
        }

        var token = _generateJSONWebToken(user, TimeSpan.FromHours(2), Permissions.All);

        return Ok(new JwtTokenDto { Token = token });
    }

    [HttpPost]
    [Route("{userId}/tokens")]
    [Authorize(Permissions.GenerateToken)]
    public async Task<ActionResult<JwtTokenDto>> GenerateToken(Guid userId, GenerateTokenDto generateTokenDto)
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

        var user = await userManager.FindByIdAsync(claimUserIdString).ConfigureAwait(false);

        if (user == null)
        {
            return Unauthorized();
        }

        if (generateTokenDto.Permissions.Any(p => p is Permissions.All or Permissions.GenerateToken) ||
            generateTokenDto.Expiry > TimeSpan.FromDays(365))
        {
            return BadRequest();
        }

        var token = _generateJSONWebToken(user, generateTokenDto.Expiry, generateTokenDto.Permissions);

        return Ok(new JwtTokenDto { Token = token });
    }

    private string _generateJSONWebToken(User userInfo, TimeSpan expiry, params string[] permissions)
    {
        var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(config["Jwt:Key"]!));
        var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.Name, userInfo.UserName!),
            new Claim(ClaimTypes.NameIdentifier, userInfo.Id),
            new Claim(Permissions.PermissionClaimType, JsonSerializer.Serialize(permissions))
        };

        var token = new JwtSecurityToken(config["Jwt:Issuer"],
            config["Jwt:Issuer"],
            claims,
            expires: DateTime.Now.Add(expiry),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}