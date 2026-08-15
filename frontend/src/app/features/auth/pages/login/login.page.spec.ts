import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthUser } from '../../../../core/models/auth.models';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let loginResult: Subject<AuthUser>;

  const authService = {
    login: vi.fn(),
  };

  const router = {
    navigateByUrl: vi.fn().mockResolvedValue(true),
  };

  const activatedRoute = {
    snapshot: {
      queryParamMap: {
        get: vi.fn().mockReturnValue(null),
      },
    },
  };

  beforeEach(async () => {
    loginResult = new Subject<AuthUser>();
    authService.login.mockReset().mockReturnValue(loginResult.asObservable());
    router.navigateByUrl.mockClear();
    activatedRoute.snapshot.queryParamMap.get.mockClear();

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: activatedRoute },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
  });

  it('renders an asynchronous backend error and re-enables submission', async () => {
    fixture.componentInstance.form.setValue({
      email: 'invalid@example.test',
      password: 'not-a-real-password',
    });

    fixture.componentInstance.submit();
    await fixture.whenStable();

    let button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Connexion...');

    loginResult.error({ error: { detail: 'Invalid credentials' } });
    await fixture.whenStable();

    const alert = fixture.nativeElement.querySelector('.alert') as HTMLElement;
    button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(alert.textContent).toContain('Invalid credentials');
    expect(button.disabled).toBe(false);
    expect(button.textContent).toContain('Se connecter');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('navigates to the requested route after a successful login', async () => {
    activatedRoute.snapshot.queryParamMap.get.mockReturnValue('/projects');
    fixture.componentInstance.form.setValue({
      email: 'user@example.test',
      password: 'not-a-real-password',
    });

    fixture.componentInstance.submit();
    loginResult.next({
      id: 7,
      email: 'user@example.test',
      role: 'USER',
      displayName: 'Test User',
    });
    loginResult.complete();
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/projects');
    expect(fixture.componentInstance.loading()).toBe(false);
  });
});
