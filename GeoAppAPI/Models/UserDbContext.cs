using GeoAppAPI.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace GeoAppAPI.Controllers;

public class UserDbContext(DbContextOptions<UserDbContext> options) : IdentityDbContext<User>(options)
{
    
}