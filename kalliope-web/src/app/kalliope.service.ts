import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Analysis, Canon, Example, Override } from './kalliope.models';

@Injectable({ providedIn: 'root' })
export class KalliopeService {
  private readonly http = inject(HttpClient);

  analyze(
    text: string,
    settings: Record<string, boolean>,
    overrides: Override[] = [],
  ): Observable<Analysis> {
    return this.http.post<Analysis>('/api/analyze', { text, settings, overrides });
  }

  canon(): Observable<Canon> {
    return this.http.get<Canon>('/api/canon');
  }

  examples(): Observable<Example[]> {
    return this.http.get<Example[]>('/api/examples');
  }
}
