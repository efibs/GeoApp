using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using GeoAppAPI.Models;
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
    public async Task<ActionResult<JwtToken>> RegisterUser([FromBody] Register register)
    {
        var user = new User
        {
            UserName = register.Username,
        };
        var result = await userManager.CreateAsync(user, register.Password).ConfigureAwait(false);
        if (!result.Succeeded)
        {
            return BadRequest(result.Errors);
        }

        var createdUser = await userManager.FindByNameAsync(register.Username).ConfigureAwait(false);

        if (createdUser == null)
        {
            return StatusCode(500);
        }
        
        var token = _generateJSONWebToken(createdUser, true);
        
        return Ok(new JwtToken{Token = token});
    }

    [HttpPost]
    [Route("login")]
    public async Task<ActionResult<JwtToken>> Login([FromBody] Login login)
    {
        var user = await userManager.FindByNameAsync(login.Username).ConfigureAwait(false);

        if (user == null)
        {
            return Unauthorized();
        }
        
        var passwordValid = await userManager.CheckPasswordAsync(user, login.Password).ConfigureAwait(false);

        if (!passwordValid)
        {
            return Unauthorized();
        }
        
        var token = _generateJSONWebToken(user, true);
        
        return Ok(new JwtToken{Token = token});
    }
    
    private string _generateJSONWebToken(User userInfo, bool fullAccess)
    {
        var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(config["Jwt:Key"]!));
        var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

        var claims = new List<Claim>();

        claims.Add(new Claim(ClaimTypes.Name, userInfo.UserName!));
        claims.Add(new Claim(ClaimTypes.NameIdentifier, userInfo.Id));
        
        if (fullAccess)
        {
            claims.Add(new Claim("FullAccess", "true"));
        }
        
        var token = new JwtSecurityToken(config["Jwt:Issuer"],
            config["Jwt:Issuer"],
            claims,
            expires: DateTime.Now.AddMinutes(120),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}